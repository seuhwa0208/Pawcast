package com.project.pawcast.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class OrderNumberGenerator {
    public static String createOrderNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomNumber = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.format("ORD-%s-%06d", date, randomNumber);
    }
}
