package eci.technician.helpers.api.retroapi

import eci.technician.helpers.AppAuth
import eci.technician.models.*
import eci.technician.models.attachments.postModels.UploadFileModel
import eci.technician.models.attachments.responses.AttachmentFile
import eci.technician.models.attachments.responses.AttachmentItem
import eci.technician.models.create_call.CallType
import eci.technician.models.create_call.CreateSC
import eci.technician.models.create_call.CustomerItem
import eci.technician.models.create_call.EquipmentItem
import eci.technician.models.equipment.EquipmentSearchModel
import eci.technician.models.field_transfer.PartRequestTransfer
import eci.technician.models.field_transfer.PostCancelOrderModel
import eci.technician.models.gps.CarInfo
import eci.technician.models.gps.UpdatePosition
import eci.technician.models.order.*
import eci.technician.models.order.Part
import eci.technician.models.parts.postModels.PartToDeletePostModel
import eci.technician.models.serviceCallNotes.postModels.CreateNotePostModel
import eci.technician.models.serviceCallNotes.postModels.DeleteNotePostModel
import eci.technician.models.serviceCallNotes.postModels.UpdateNotePostModel
import eci.technician.models.time_cards.ChangeStatusModel
import eci.technician.models.transfers.CreateTransferModel
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface MobileTechApi {

    @POST("User/Login")
    suspend fun loginMobileTech(@Body loginPostModel: LoginPostModel): Response<ProcessingResult>

    @POST("User/UpdateDeviceToken")
    suspend fun sendDeviceToken(@Body deviceToken: DeviceTokenPostModel): Response<ProcessingResult>

    @POST("Technician/{action}")
    suspend fun requestCallAction2(
        @Path("action") action: String,
        @Body map: HashMap<String, Any>
    ): Response<ProcessingResult>


    @POST("Technician/OnHoldCall")
    suspend fun requestOnHoldAction2(@Body statusChangeModel: StatusChangeModel): Response<ProcessingResult>


    @POST("Technician/{action}")
    suspend fun requestClockStuff2(
        @Path("action") action: String,
        @Body changeStatusModel: ChangeStatusModel
    ): Response<ProcessingResult>


    @POST("Technician/ScheduleCall")
    suspend fun requestScheduleCall2(@Body statusChangeModel: StatusChangeModel): Response<ProcessingResult>

    @POST("Technician/{action}")
    fun completeCall(
        @Path("action") action: String,
        @Body statusChangeModel: StatusChangeModel
    ): Call<ProcessingResult>

    @POST("Technician/{action}")
    suspend fun completeCall2(
        @Path("action") action: String,
        @Body statusChangeModel: StatusChangeModel
    ): Response<ProcessingResult>


    @POST("Technician/HoldRelease")
    suspend fun requestHoldRelease2(@Body statusChangeModel: StatusChangeModel): Response<ProcessingResult>

    @POST("Technician/CancelCall")
    suspend fun cancelCall(@Body statusChangeModel: StatusChangeModel): Response<ProcessingResult>

    @GET("ServiceCall/GetTechnicianActiveServiceCalls")
    suspend fun getAllTechnicianActiveServiceCallsSuspend(@Query("lastUpdate") lastUpdate: String? = null): Response<MutableList<ServiceOrder>>

    @GET("ServiceCall/GetAllEquipmentMeters")
    suspend fun getAllEquipmentMeters(@Query("technicianId") technicianId: Int): Response<MutableList<EquipmentRealmMeter>>

    @GET("ServiceCall/GetAllEquipmentMeters")
    suspend fun getAllEquipmentMeters2(@Query("technicianId") technicianId: Int): Response<List<EquipmentRealmMeter>>

    @GET("ServiceCall/GetEquipmentMeters")
    fun getEquipmentMeterByEquipmentId(@Query("equipmentId") equipmentId: Int): Call<MutableList<EquipmentRealmMeter>>

    @POST("ServiceCall/{action}")
    suspend fun updateEquipmentDetails2(
        @Path("action") action: String,
        @Body map: HashMap<String, Any>
    ): Response<ProcessingResult>

    @GET("User/Technician/UserInfo")
    fun updateUser(): Call<ProcessingResult>

    @GET("User/Technician/UserInfo")
    suspend fun updateUser2(): Response<ProcessingResult>

    @GET("ServiceCall/GetAllIncompleteCodes")
    fun getAllIncompleteCodes(): Call<MutableList<IncompleteCode>>

    @GET("ServiceCall/GetAllIncompleteCodes")
    suspend fun getAllIncompleteCodes2(): Response<List<IncompleteCode>>


    @GET("Technician/GetGroupsByUserId")
    fun getGroupByUserId(@Query("userId") userId: String): Call<ProcessingResult?>

    @GET("Technician/GetGroupsByUserId")
    suspend fun getGroupByUserId2(@Query("userId") userId: String): Response<ProcessingResult>

    @GET("ServiceCall/GetActiveServiceCalls")
    fun getGroupServiceCallListWithFilterType(
        @Query("groupFilterType") groupFilterType: Int,
        @Query("groupName") groupName: String?
    ): Call<MutableList<GroupCallServiceOrder>>

    @POST("Technician/IncompleteCall")
    fun incompleteServiceCallPrimary(@Body statusChangeModel: StatusChangeModel): Call<ProcessingResult>

    @GET("ServiceCall/GetHoldCodes")
    fun getHoldCodes(): Call<MutableList<HoldCode>>

    @GET("ServiceCall/GetHoldCodes")
    suspend fun getHoldCodes2(): Response<List<HoldCode>>

    @POST("Technician/SignIn")
    suspend fun clockInUser(@Body changeStatusModel: ChangeStatusModel): Response<ProcessingResult>

    @POST("Technician/SignOut")
    suspend fun clockOutUser(@Body changeStatusModel: ChangeStatusModel): Response<ProcessingResult>

    @POST("Technician/ReassignServiceCall")
    fun reassignServiceCall(@Body map: HashMap<String, Any>): Call<ProcessingResult?>

    @GET("Technician/GetAllTechnicians")
    suspend fun getAllTechnicians(): Response<ProcessingResult>

    @POST("Technician/AddRequestMaterial")
    suspend fun addRequestMaterial(@Body listOfMaterial: MutableList<RequestPart>): Response<ProcessingResult>

    @POST("Technician/GetPayPeriodShifts")
    suspend fun getPayPeriodsShiftsByPayPeriod(@Body map: MutableMap<String, Any>): Response<ProcessingResult>

    @POST("Technician/GetShiftDetails")
    suspend fun getShiftDetailsByShiftId(@Body shiftId: String): Response<ProcessingResult>

    @POST("ServiceCall/UploadEsnDocument")
    suspend fun uploadFile2(@Body uploadFileModel: UploadFileModel): Response<ProcessingResult>

    @GET("ServiceCall/GetAttachments")
    suspend fun getAttachments2(@Query("callNumber") callNumber: String): Response<List<AttachmentItem>?>

    @GET("ServiceCall/GetAttachmentFile")
    suspend fun getAttachmentFile(@Query("id") id: Int): Response<AttachmentFile>

    @GET("CallType/GetAllCallTypes")
    fun getAllCallTypes(): Call<MutableList<CallType>>

    @GET("CallType/GetAllCallTypes")
    suspend fun getAllCallTypes2(): Response<List<CallType>>

    @GET("ServiceCall/GetCustomerDataByText")
    fun getCustomerDataByText(@Query("searchText") searchText: String): Call<MutableList<CustomerItem>>

    @GET("ServiceCall/GetCustomerDataByText")
    suspend fun getCustomerDataByTextSuspend(@Query("searchText") searchText: String?): Response<MutableList<CustomerItem>?>

    @GET("ServiceCall/GetEquipmentsDataByText")
    fun getEquipmentsDataByText(@Query("searchText") searchText: String): Call<MutableList<EquipmentItem>>

    @GET("ServiceCall/GetEquipmentsDataByText")
    suspend fun getEquipmentsDataByTextSuspend(@Query("searchText") searchText: String?): Response<MutableList<EquipmentItem>?>

    @POST("ServiceCall/NewServiceCall")
    fun createNewServiceCall(@Body createSCBody: CreateSC): Call<ProcessingResult>

    @GET("ServiceCall/GetServiceCallProblemCodes")
    fun getServiceCalProblemCodes(): Call<MutableList<ProblemCode>>

    @GET("ServiceCall/GetServiceCallProblemCodes")
    suspend fun getServiceCalProblemCodes2(): Response<List<ProblemCode>>

    @GET("ServiceCall/GetServiceCallRepairCodes")
    fun getServiceCallRepairCodes(): Call<MutableList<RepairCode>>

    @GET("ServiceCall/GetServiceCallRepairCodes")
    suspend fun getServiceCallRepairCodes2(): Response<List<RepairCode>>

    @GET("Technician/GetAllActivityCallTypes")
    fun getAllActivityCallTypes(): Call<ProcessingResult>

    @GET("Technician/GetAllActivityCallTypes")
    suspend fun getAllActivityCallTypes2(): Response<ProcessingResult>

    @GET("Technician/GetAllAssistanceList")
    suspend fun getAllAssistanceList(): Response<ProcessingResult>

    @GET("ServiceCall/GetCancelCodes")
    fun getCancelCodes(): Call<MutableList<CancelCode>>

    @GET("ServiceCall/GetCancelCodes")
    suspend fun getCancelCodes2(): Response<List<CancelCode>>

    @GET("ServiceCall/GetTechnicianCompletedServiceCalls")
    fun getTechnicianCompletedServiceCalls(): Call<MutableList<CompletedServiceOrder>?>

    @GET("Technician/GetPartRequestsFromMe")
    fun getPartsRequestsFromMe(): Call<MutableList<PartRequestTransfer>>

    /**
     * Field Transfer Requests
     */

    @GET("Technician/GetPartRequestsFromMe")
    suspend fun getPartsRequestsFromMe2(): Response<List<PartRequestTransfer>>

    @GET("Technician/GetMyPartRequests")
    suspend fun getMyPartRequest(): Response<List<PartRequestTransfer>>

    @POST("Technician/CancelTransferOrder")
    suspend fun cancelTransferOrder(@Body cancelModel: PostCancelOrderModel): Response<ProcessingResult>

    @POST("Technician/PostTransferOrder")
    suspend fun postTransferOrder(@Body value: Int): Response<ProcessingResult>

    @POST("Technician/PutTransferOrder")
    suspend fun putTransferOrder(@Body map: HashMap<String, Any>): Response<ProcessingResult>

    @GET("ServiceCall/GetPartsInTechniciansWarehouses")
    suspend fun getPartsInTechniciansWarehouses(
        @Query("itemId") itemId: String?,
        @Query("available") available: String? = true.toString()
    ): Response<List<RequestPartTransferItem>>

    @GET("ServiceCall/GetOnlyTechWarehouseParts")
    suspend fun getOnlyTechWarehouseParts(
        @Query("available") available: String = false.toString(),
        @Query("partCode") partCode: String = ""
    ): Response<List<Part>>

    /**
     * End Field Transfers requests
     */

    @GET("ServiceCall/GetEquipmentHistory")
    fun getEquipmentHistoryByEquipmentId(@Query("equipmentId") equipmentId: String): Call<MutableList<EquipmentHistoryModel>>

    @GET("ServiceCall/UpdateEquipmentHistory")
    fun updateEquipmentHistoryByEquipmentId(@Query("equipmentId") equipmentId: String): Call<MutableList<EquipmentHistoryModel>>

    @GET("ServiceCall/GetEquipmentsByText")
    fun getEquipmentByText(@Query("searchText") searchText: String): Call<MutableList<EquipmentSearchModel>>

    @POST("Technician/AddAssistance")
    fun addAssistance(@Body map: HashMap<String, Any>): Call<ProcessingResult>

    /**
     * Used for Search Warehouses Screen
     * Returns all the Parts available from all the Warehouses
     * without the binsInfo because of the big amount of data
     */
    @Streaming
    @GET("ServiceCall/GetAvailableParts")
    suspend fun getAllAvailablePartsFromAllWarehouses(
        @Query("partCode") partCode: String = "",
        @Query("available") available: Boolean = true,
        @Query("fulllist") fulList: Boolean = false
    ): Response<List<Part>>

    /**
     * Used for Request Parts AND Needed Parts (should show ALL the parts)
     * Returns all the parts(Available and Unavailable) from all the Warehouses
     * without the binsInfo because of the big amount of data
     */
    @Streaming
    @GET("ServiceCall/GetAvailablePartsFullList")
    suspend fun getAllPartsFromAllWarehouses(
        @Query("partCode") partCode: String = "",
        @Query("available") available: Boolean = false
    ): Response<ProcessingResult>

    @POST("Technician/UpdateLabor")
    fun updateLaborForAssist(@Body updateLaborPostModel: UpdateLaborPostModel): Call<ProcessingResult>

    @GET("ServiceCall/GetServiceCallByCallId")
    fun getOneServiceCallById(@Query("callId") callId: Int): Call<MutableList<ServiceOrder>>

    @GET("ServiceCall/GetServiceCallByCallId")
    suspend fun getOneServiceCallByIdSuspend(@Query("callId") callId: Int): Response<List<ServiceOrder>>

    @GET("ServiceCall/GetServiceCallByCallId")
    fun getOneGroupCallServiceByCallId(@Query("callId") callId: Int): Call<MutableList<GroupCallServiceOrder>>

    @GET("ServiceCall/GetServiceCallNoteTypes")
    fun getServiceCallNoteTypes(): Call<ProcessingResult?>

    @GET("ServiceCall/GetServiceCallNoteTypes")
    suspend fun getServiceCallNoteTypes2(): Response<ProcessingResult?>

    @GET("ServiceCall/GetServiceCallNoteByCallId")
    fun getServiceCallNotesByCallId(@Query("callId") callId: Int): Call<ProcessingResult?>

    @POST("ServiceCall/UpdateServiceCallNotes")
    fun updateServiceCallNote(@Body updateNotePostModel: UpdateNotePostModel): Call<ProcessingResult?>

    @POST("ServiceCall/CreateServiceCallNotes")
    fun createServiceCallNote(@Body createNotePostModel: CreateNotePostModel): Call<ProcessingResult>

    @POST("ServiceCall/DeleteServiceCallNotes")
    fun deleteServiceCallNoteByDetailId(@Body deleteNotePostModel: DeleteNotePostModel): Call<ProcessingResult>

    @GET("Technician/GetCanonDetails")
    fun getCanonDetails(
        @Query("equipmentId") equipmentId: String,
        @Query("callNumber") callNumber: String
    ): Call<ProcessingResult>

    @POST("Technician/DeleteMaterialFromCall")
    fun deleteMaterialFromCall(@Body partsToDelete: MutableList<PartToDeletePostModel>): Call<ProcessingResult>

    @GET("ServiceCall/GetServiceCallPartsByCallId")
    suspend fun getUsedPartsByCallId(@Query("callId") callId: Int): Response<ProcessingResult>

    @GET("Priority/GetAllPriorities")
    fun getPriorities(): Call<MutableList<CallPriority>>

    @GET("Priority/GetAllPriorities")
    suspend fun getPriorities2(): Response<List<CallPriority>>

    /**
     * Used for MyWarehouse screen and to add Pending/UsedParts
     * @param[warehouseId]  send this id for MyWarehouse Screen
     */
    @GET("ServiceCall/GetAvailablePartsByWarehouse")
    suspend fun getAvailablePartsByWarehouseForCurrentTech2(
        @Query("WarehouseId") warehouseId: Int = 0,
        @Query("partCode") query: String = "",
        @Query("AvailableQuantity") availableQtty: Boolean = true,
        @Query("AvailableInTechWarehouse") availableInTechWarehouse: Boolean = false,
        @Query("AvailableInLinkedWarehouse") availableInLinked: Boolean = true,
        @Query("lastUpdate") lastUpdate: String? = null
    ): Response<ProcessingResult>

    /**
     * Used for compatibility between app and backend
     * returns backend version
     */
    @GET("User/GetEciHostVersion")
    suspend fun getEciHostVersion(): Response<ProcessingResult>

    @GET("ServiceCall/GetWarehouses")
    fun getAllWarehouses(): Call<ProcessingResult>

    /*
        Used  in transfer feature(TransfersViewModel)
        Recovers all the warehouses that the tech is allowed to see,
        if there is a part it will send the its id, in order to get the warehouses with its default bins
     */
    @GET("ServiceCall/GetWarehouses")
    suspend fun getAllWarehouses2(
        @Query("GetDefaultBinForItemId") partId: Int = -1
    ): Response<ProcessingResult>

    @GET("ServiceCall/GetAvailablePartsByWarehouse?partCode=&AvailableQuantity=true&AvailableInTechWarehouse=false&AvailableInAllTechWarehouse=false&AvailableInLinkedWarehouse=true")
    fun getWarehousePartsById(@Query("WarehouseID") idWarehouse: String): Call<ProcessingResult>

    @POST("Technician/InventoryTransfer")
    fun createNewPartTransfer(
        @Body createTransferModel: CreateTransferModel,
    ): Call<ProcessingResult>

    @POST("Technician/UpdateGpsLocation")
    fun updateGpsLocation(
        @Body updatePosition: UpdatePosition
    ): Call<ProcessingResult>

    @GET("Car/Check")
    fun checkCar(
        @Query("ident") id: String = AppAuth.getInstance().gpsPrefix + AppAuth.getInstance().technicianUser.technicianCode,
        @Query("type") type: String = "1"
    ): Call<CarInfo>

    @GET("TimeCard/GetTimeCardEntry/{date}")
    suspend fun getTimeCardsEntry(@Path("date") date: String): Response<ProcessingResult>
}