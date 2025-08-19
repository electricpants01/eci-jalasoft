package eci.technician.helpers;

import static android.content.Context.LOCATION_SERVICE;
import static android.content.Context.MODE_PRIVATE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;

import androidx.annotation.Nullable;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eci.signalr.TechnicianConnection;
import eci.signalr.messenger.Conversation;
import eci.signalr.messenger.ConversationUser;
import eci.signalr.messenger.Message;
import eci.technician.MainApplication;
import eci.technician.adapters.ConversationsAdapter;
import eci.technician.helpers.api.ApiHelper;
import eci.technician.helpers.api.ApiHelperBuilder;
import eci.technician.helpers.api.retroapi.RetrofitApiHelper;
import eci.technician.models.LastUserModel;
import eci.technician.models.ProcessingResult;
import eci.technician.models.TechnicianUser;
import eci.technician.models.attachments.persistModels.AttachmentItemEntity;
import eci.technician.models.data.UsedPart;
import eci.technician.models.data.UsedProblemCode;
import eci.technician.models.data.UsedRepairCode;
import eci.technician.models.field_transfer.PartRequestTransfer;
import eci.technician.models.attachments.persistModels.AttachmentIncompleteRequest;
import eci.technician.models.attachments.responses.AttachmentItem;
import eci.technician.models.order.CancelCode;
import eci.technician.models.order.CompletedServiceOrder;
import eci.technician.models.order.HoldCode;
import eci.technician.models.order.IncompleteRequests;
import eci.technician.models.order.PartUsageStatus;
import eci.technician.models.order.ProblemCode;
import eci.technician.models.order.RepairCode;
import eci.technician.models.order.ServiceOrder;
import eci.technician.repository.DatabaseRepository;
import eci.technician.repository.LoginRepository;
import eci.technician.tools.Constants;
import io.realm.Realm;
import io.realm.RealmResults;

public class AppAuth {
    private static String TAG = "AppAuth";
    private static String EXCEPTION = "Exception";
    private static AppAuth INSTANCE;
    private final List<AuthStateListener> authStateListeners;
    private final Handler handler;
    private final SharedPreferences preferences;
    private TechnicianUser technicianUser;
    private String token = "";
    private String serverAddress = "";
    private String signalRServerAddress = "";
    private Boolean chatEnabled = false;
    private int offlineCounter = 0;
    private String gpsServer = "";
    private String gpsPrefix = "";
    private boolean gpsProviderEnabled;
    private FirebaseAnalytics firebaseAnalytics;
    public Context context;
    private int completedCallId;
    private boolean connected;
    private int syncCounter = 0;

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    private SparseBooleanArray requests = new SparseBooleanArray();

    public ConversationsAdapter getConversationsAdapter() {
        return conversationsAdapter;
    }

    public void setConversationsAdapter(ConversationsAdapter conversationsAdapter) {
        this.conversationsAdapter = conversationsAdapter;
    }

    private ConversationsAdapter conversationsAdapter;

    public boolean isReconnecting() {
        return isReconnecting;
    }

    public void setReconnecting(boolean reconnecting) {
        isReconnecting = reconnecting;
    }

    private boolean isReconnecting = true;

    private int newRequestsCount = 0;

    private AppAuth(Context context) {
        this.context = context;
        authStateListeners = new ArrayList<>();
        handler = new Handler(Looper.getMainLooper());
        preferences = context.getSharedPreferences(Constants.PREFERENCE_AUTH, MODE_PRIVATE);
        firebaseAnalytics = FirebaseAnalytics.getInstance(context);
//        realm = Realm.getDefaultInstance();

        if (preferences.contains(Constants.PREFERENCE_LAST_ODOMETER)) {
            float lastFloatOdometer = preferences.getFloat(Constants.PREFERENCE_LAST_ODOMETER, 0F);
            preferences.edit().putInt(Constants.PREFERENCE_LAST_ODOMETER_INT, (int) lastFloatOdometer).apply();
            preferences.edit().remove(Constants.PREFERENCE_LAST_ODOMETER).apply();
        }

        if (preferences.contains(Constants.PREFERENCE_SERVER_ADDRESS)) {
            serverAddress = preferences.getString(Constants.PREFERENCE_SERVER_ADDRESS, "");
        }

        if (preferences.contains(Constants.PREFERENCE_SIGNAL_R_SERVER_ADDRESS)) {
            signalRServerAddress = preferences.getString(Constants.PREFERENCE_SIGNAL_R_SERVER_ADDRESS, "");
        }

        if (preferences.contains(Constants.PREFERENCE_CHAT_ENABLED)) {
            chatEnabled = preferences.getBoolean(Constants.PREFERENCE_CHAT_ENABLED, false);
        }

        if (preferences.contains(Constants.PREFERENCE_GPS_SERVER_ADDRESS)) {
            gpsServer = preferences.getString(Constants.PREFERENCE_GPS_SERVER_ADDRESS, "");
        }

        if (preferences.contains(Constants.PREFERENCE_GPS_PREFIX)) {
            gpsPrefix = preferences.getString(Constants.PREFERENCE_GPS_PREFIX, "");
        }

        if (preferences.contains(Constants.PREFERENCE_TOKEN)) {
            setToken(preferences.getString(Constants.PREFERENCE_TOKEN, ""));
        }

        if (preferences.contains(Constants.PREFERENCE_USER)) {
            TechnicianUser authUser = new Gson().fromJson(preferences.getString(Constants.PREFERENCE_USER, ""), TechnicianUser.class);
            setTechnicianUser(authUser);
        }

        BroadcastReceiver gpsStateChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
                boolean providerEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                setGpsProviderEnabled(providerEnabled);
            }
        };

        LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        gpsProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        context.registerReceiver(gpsStateChangeReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
    }

    public static synchronized AppAuth getInstance() {
        if (INSTANCE == null) {
            throw new RuntimeException("Instance not initialized");
        }
        return INSTANCE;
    }

    public static synchronized void init(Context context) {
        INSTANCE = new AppAuth(context);
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getSyncCounter() {
        return syncCounter;
    }

    public void setSyncCounter(int syncCounter) {
        this.syncCounter = syncCounter;
    }

    public boolean isLoggedIn() {
        return technicianUser != null;
    }

    public TechnicianUser getTechnicianUser() {
        return technicianUser;
    }

    public void setTechnicianUser(TechnicianUser technicianUser) {
        if ((this.technicianUser == null && technicianUser != null) || (this.technicianUser != null && technicianUser == null)) {
            notifyAllAuthStateChanged();
        }
        this.technicianUser = technicianUser;
        saveUser(technicianUser);
    }

    public int getNewRequestsCount() {
        return newRequestsCount;
    }

    public void requestNumberUpdate() {
        newRequestsCount -= 1;
    }

    public boolean checkLicenseAgreement() {
        return preferences.getInt(Constants.PREFERENCE_LICENSE_AGREEMENT, 0) > 0;
    }

    public void acceptLicenseAgreement() {
        preferences
                .edit()
                .putInt(Constants.PREFERENCE_LICENSE_AGREEMENT, 1)
                .apply();
    }

    private void setGpsProviderEnabled(boolean providerEnabled) {
        if (providerEnabled != gpsProviderEnabled) {
            gpsProviderEnabled = providerEnabled;
            for (AuthStateListener listener : authStateListeners) {
                listener.gpsStateChanged(gpsProviderEnabled);
            }
            sendGpsStatus();
        }
    }

    public void sendGpsStatus() {
        if (!isLoggedIn()) {
            return;
        }
        ApiHelper<Void> apiHelper = new ApiHelperBuilder<>(Void.class)
                .addPath("Technician")
                .addPath("GpsStatus")
                .addParameter("status", String.valueOf(gpsProviderEnabled))
                .setAuthorized(true)
                .build();

        apiHelper.runAsync(new ApiHelper.ApiRequestListener<Void>() {
            @Override
            public void complete(boolean success, Void result, int errorCode, String errorMessage) {
            }
        });
    }

    public void saveLicenseLoginData(
            String serverAddress,
            String signalRServerAddress,
            Boolean chatEnabled,
            String gpsServer,
            String gpsPrefix) {
        this.serverAddress = serverAddress;
        this.signalRServerAddress = signalRServerAddress;
        this.chatEnabled = chatEnabled;
        this.gpsServer = gpsServer;
        this.gpsPrefix = gpsPrefix;

        preferences.edit()
                .putString(Constants.PREFERENCE_SERVER_ADDRESS, serverAddress)
                .putString(Constants.PREFERENCE_SIGNAL_R_SERVER_ADDRESS, signalRServerAddress)
                .putBoolean(Constants.PREFERENCE_CHAT_ENABLED, chatEnabled)
                .putString(Constants.PREFERENCE_GPS_SERVER_ADDRESS, gpsServer)
                .putString(Constants.PREFERENCE_GPS_PREFIX, gpsPrefix)
                .apply();
    }


    public void signOut() {
        setLastOdometer(0);
        Map<String, Object> map = new HashMap<>();
        map.put("UserId", AppAuth.getInstance().getTechnicianUser().getId());
        map.put("DeviceToken", "");
        map.put("DeviceType", "1");
        ApiHelper<ProcessingResult> apiHelper = new ApiHelperBuilder<>(ProcessingResult.class)
                .addPath("User")
                .addPath("UpdateDeviceToken")
                .setMethodPost(map)
                .setAuthorized(true)
                .build();
        apiHelper.runAsync(new ApiHelper.ApiRequestListener<ProcessingResult>() {
            @Override
            public void complete(boolean success, ProcessingResult result, int errorCode, String errorMessage) {
            }
        });

        RetrofitApiHelper.INSTANCE.setApiToNull();
        setToken("");
        deletePreferencesForFilters();
        deleteMapPreferences();
        DatabaseRepository.getInstance().deleteDataOnLogout();
        deleteFilesDownloaded();
        if (AppAuth.getInstance().chatEnabled) {
            Realm realm1 = Realm.getDefaultInstance();
            try{
                final RealmResults<Conversation> conversations = realm1.where(Conversation.class).findAll();
                realm1.executeTransaction(realm -> {
                    conversations.deleteAllFromRealm();
                    conversationsAdapter.notifyDataSetChanged();
                });
            }catch (Exception e){
                Log.e(TAG, EXCEPTION, e);
            }finally {
                realm1.close();
            }

            DatabaseRepository.getInstance().deleteDataOnLogout();
        }
    }

    private void deleteMapPreferences() {
        context.getSharedPreferences(Constants.PREFERENCE_NAVIGATION, MODE_PRIVATE).edit().clear().apply();
    }

    private void deleteFilesDownloaded() {
        try {
            File fileOrDirectory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (fileOrDirectory.isDirectory()) {
                File[] list = fileOrDirectory.listFiles();
                if (list != null) {
                    for (File child : list) {
                        child.delete();
                    }
                }
            } else {
                fileOrDirectory.delete();
            }
        } catch (Exception e) {
            Log.d(TAG, EXCEPTION, e);
        }
    }

    public void deletePreferencesForFilters() {
        AppAuth.getInstance().setGroupNameFilterForGroupCalls("");
        AppAuth.getInstance().saveDateFilterForGroupCalls(FilterHelper.DateFilter.NOT_SELECTED);
        AppAuth.getInstance().setDateFilterForServiceOrderList(FilterHelper.DateFilter.NOT_SELECTED);
        AppAuth.getInstance().setStatusCallFilterForServiceOrderList(FilterHelper.ServiceCallStatus.NOT_SELECTED);
    }

    public void setServerAddress(@Nullable String newServerAddress) {
        this.serverAddress = newServerAddress;
    }

    private void saveUser(TechnicianUser technicianUser) {
        if (technicianUser == null) {
            preferences.edit()
                    .remove(Constants.PREFERENCE_USER)
                    .apply();
        } else {
            String lastLoggedInUser = preferences.getString(Constants.PREFERENCE_LAST_LOGGED_IN_USER, "");
            if (!TextUtils.equals(lastLoggedInUser, technicianUser.getId())) {
                clearDatabase();
            }

            preferences.edit()
                    .putString(Constants.PREFERENCE_USER, new Gson().toJson(technicianUser))
                    .putString(Constants.PREFERENCE_LAST_LOGGED_IN_USER, technicianUser.getId())
                    .apply();
        }
    }

    public int getLastOdometer() {
        return preferences.getInt(Constants.PREFERENCE_LAST_ODOMETER_INT, 0);
    }

    public void setLastOdometer(int value) {
        preferences.edit().putInt(Constants.PREFERENCE_LAST_ODOMETER_INT, value).apply();
    }

    public void saveDateFilterForGroupCalls(FilterHelper.DateFilter whenFilterGroupCalls) {
        preferences.edit().putString(Constants.PREFERENCE_GROUP_CALLS_WHEN_FILTER, whenFilterGroupCalls.name()).apply();
    }

    public String getDateFilterForGroupCalls() {
        return preferences.getString(Constants.PREFERENCE_GROUP_CALLS_WHEN_FILTER, "");
    }

    public void setGroupNameFilterForGroupCalls(String groupName) {
        preferences.edit().putString(Constants.PREFERENCE_GROUP_CALLS_GROUP_FILTER, groupName).apply();
    }

    public String getGroupNameFilterForGroupCalls() {
        return preferences.getString(Constants.PREFERENCE_GROUP_CALLS_GROUP_FILTER, "");
    }

    public void setDateFilterForServiceOrderList(FilterHelper.DateFilter whenFilterServiceOrderList) {
        preferences.edit().putString(Constants.PREFERENCE_SERVICE_CALLS_WHEN_FILTER, whenFilterServiceOrderList.name()).apply();
    }

    public String getDateFilterForServiceOrderList() {
        return preferences.getString(Constants.PREFERENCE_SERVICE_CALLS_WHEN_FILTER, FilterHelper.DateFilter.NOT_SELECTED.name());
    }

    public void setStatusCallFilterForServiceOrderList(FilterHelper.ServiceCallStatus status) {
        preferences.edit().putString(Constants.PREFERENCE_SERVICE_CALLS_STATUS_FILTER, status.name()).apply();
    }

    public String getStatusCallFilterForServiceOrderList() {
        return preferences.getString(Constants.PREFERENCE_SERVICE_CALLS_STATUS_FILTER, FilterHelper.ServiceCallStatus.NOT_SELECTED.name());
    }

    public void setLocationDeniedSelected(Boolean isSelected) {
        preferences.edit().putBoolean(Constants.PREFERENCE_LOCATION_DENIED, isSelected).apply();
    }

    public boolean getLocationDeniedSelected() {
        try {
            return preferences.getBoolean(Constants.PREFERENCE_LOCATION_DENIED, false);
        } catch (Exception e) {
            return false;
        }
    }

    public void setRequestHasBeenSeen(boolean hasBeenSeen) {
        preferences.edit().putBoolean(Constants.PREFERENCE_PART_REQUEST_HAS_BEEN_SEEN, hasBeenSeen).apply();
    }

    public boolean getRequestHasBeenSeen() {
        try {
            return preferences.getBoolean(Constants.PREFERENCE_PART_REQUEST_HAS_BEEN_SEEN, false);
        } catch (Exception e) {
            return false;
        }
    }

    public void setSetForPartsRequestFromMe(Set<String> set) {
        preferences.edit().putStringSet(Constants.PREFERENCE_SET_OF_PART_REQUEST, set).apply();
    }

    public Set<String> getPartsRequestFromMeSet() {
        try {
            Set<String> set = preferences.getStringSet(Constants.PREFERENCE_SET_OF_PART_REQUEST, null);
            if (set == null) {
                return new HashSet<>();
            } else {
                return set;
            }
        } catch (Exception e) {
            return new HashSet<>();
        }
    }

    private void clearDatabase() {
        Realm realm = Realm.getDefaultInstance();
        try {
            realm.executeTransaction(realm1 -> {
                realm1.delete(Message.class);
                realm1.delete(Conversation.class);
                realm1.delete(ConversationUser.class);
                realm1.delete(UsedPart.class);
                realm1.delete(UsedProblemCode.class);
                realm1.delete(UsedRepairCode.class);
                realm1.delete(ServiceOrder.class);
                realm1.delete(CompletedServiceOrder.class);
                realm1.delete(PartUsageStatus.class);
                realm1.delete(ProblemCode.class);
                realm1.delete(RepairCode.class);
                realm1.delete(HoldCode.class);
                realm1.delete(CancelCode.class);
                realm1.delete(IncompleteRequests.class);
                realm1.delete(AttachmentIncompleteRequest.class);
                realm1.delete(AttachmentItemEntity.class);
            });
        } catch (Exception e) {
            Log.e(TAG, EXCEPTION, e);
        } finally {
            realm.close();
        }
    }

    private void notifyAllAuthStateChanged() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (AuthStateListener listener : authStateListeners) {
                    listener.authStateChanged(AppAuth.this);
                }
            }
        });
    }

    public void connectSignalR() {
        try {
            MainApplication.setConnection(new TechnicianConnection());
            if (MainApplication.getConnection() != null) {
                MainApplication.getConnection().initialize(signalRServerAddress);
                MainApplication.getConnection().setToken(token);
                MainApplication.getConnection().connect();
            }
        } catch (Exception e) {
            Log.e(TAG, EXCEPTION, e);
        }
    }

    private void disconnectSignalR() {
        try {
            if (MainApplication.getConnection() != null) {
                MainApplication.getConnection().disconnect();
            }
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Log.e(TAG, EXCEPTION, e);
        }

    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
        if (token.isEmpty()) {
            if (technicianUser != null) {
                preferences.edit()
                        .putString(Constants.PREFERENCE_LAST_LOGGED_IN_USER, technicianUser.getId())
                        .apply();
            }
            setTechnicianUser(null);
            resetNewRequestsCount();
            requests.clear();
            preferences.edit()
                    .remove(Constants.PREFERENCE_TOKEN)
                    .remove(Constants.PREFERENCE_GPS_PREFIX)
                    .remove(Constants.PREFERENCE_GPS_SERVER_ADDRESS)
                    .remove(Constants.PREFERENCE_SIGNAL_R_SERVER_ADDRESS)
                    .remove(Constants.PREFERENCE_SERVER_ADDRESS)
                    .remove(Constants.PREFERENCE_PART_REQUEST_HAS_BEEN_SEEN)
                    .remove(Constants.PREFERENCE_SET_OF_PART_REQUEST)
                    .apply();
            disconnectSignalR();
        } else {
            preferences.edit()
                    .putString(Constants.PREFERENCE_TOKEN, token)
                    .apply();
            connectSignalR();
        }
    }

    public void changeUserStatus(String status) {
        if (status.equals(Constants.STATUS_SIGNED_IN)) {
            LoginRepository.INSTANCE.getUserInfoAux();
        }
        technicianUser.setStatus(status);
        saveUser(technicianUser);
        notifyAllStatusChanged();
    }


    public void processRequestPartsRequestsFromMe(List<PartRequestTransfer> parts) {
        Set<String> setSaved = getPartsRequestFromMeSet();
        boolean hasSeenAll = true;
        for (PartRequestTransfer part : parts) {
            if (!setSaved.contains(part.getToID().toString())) {
                hasSeenAll = false;
            }
        }
        newRequestsCount = parts.size();
        setRequestHasBeenSeen(hasSeenAll);
        notifyRequestCountChanged();
    }

    public void resetNewRequestsCount() {
        int size = requests.size();
        for (int i = 0; i < size; i++) {
            requests.put(requests.keyAt(i), true);
        }
        newRequestsCount = 0;
        notifyRequestCountChanged();
    }

    private void notifyAllStatusChanged() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (AuthStateListener listener : authStateListeners) {
                    listener.userUpdated(technicianUser);
                }
            }
        });
    }

    private void notifyRequestCountChanged() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (AuthStateListener listener : authStateListeners) {
                    listener.requestsChanged(newRequestsCount);
                }
            }
        });
    }

    public void addUserUpdateListener(AuthStateListener listener) {
        authStateListeners.add(listener);
        listener.authStateChanged(this);
        if (technicianUser != null) {
            listener.userUpdated(technicianUser);
            listener.gpsStateChanged(gpsProviderEnabled);
        }
    }

    public void removeUserUpdateListener(AuthStateListener listener) {
        authStateListeners.remove(listener);
    }

    public LastUserModel getLastUser() {
        return new Gson().fromJson(preferences.getString(Constants.PREFERENCE_LAST_USER, "{}"), LastUserModel.class);
    }

    public void setLastUser(LastUserModel model) {
        if (!model.isSave()) {
            model.setPassword("");
        }
        preferences.edit().putString(Constants.PREFERENCE_LAST_USER, new Gson().toJson(model)).apply();
    }

    public String getGpsServer() {
        return gpsServer;
    }

    public String getGpsPrefix() {
        return gpsPrefix;
    }

    public Boolean getChatEnabled() {
        return chatEnabled;
    }

    public int getOfflineCounter() {
        return offlineCounter;
    }

    public void setOfflineCounter(int offlineCounter) {
        this.offlineCounter = offlineCounter;
    }

    public interface AuthStateListener {
        void authStateChanged(AppAuth appAuth);

        void userUpdated(TechnicianUser technicianUser);

        void gpsStateChanged(boolean state);

        void requestsChanged(int count);
    }

    public interface SignInResultListener {
        void onComplete(boolean success, int errorCode, String errorMessage, TechnicianUser technicianUser);
    }

    public int getCompletedCallId() {
        return completedCallId;
    }

    public void setCompletedCallId(int completedCallId) {
        this.completedCallId = completedCallId;
    }
}