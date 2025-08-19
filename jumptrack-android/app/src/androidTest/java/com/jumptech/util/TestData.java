package com.jumptech.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TestData {

    private static final String USERS_FILE = "users.csv";

    public List<TestUser> users() {
        List<TestUser> users = new ArrayList<>();
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(USERS_FILE)) {
            Scanner scanner = new Scanner(inputStream);
            while (scanner.hasNextLine()) {
                String[] values = scanner.nextLine().split(",");
                users.add(new TestUser(values[0], values[1], Boolean.parseBoolean(values[2])));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return users;
    }

}

