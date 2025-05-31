//package com.terminal_devilal.controllers;
//
//import java.io.IOException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.terminal_devilal.Utils.FetchNSEAPI;
//
//
//public class FetchPriceVolumeDeliverablesData {
//	
//	public static String FetchPriceVolumeDeliverablesData(String ticker) throws IOException, InterruptedException {
//        // Format today's date as dd-MM-yyyy (like '05-05-2025')
////        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
//        
//        String url = String.format(
//            "https://www.nseindia.com/api/historical/securityArchives?from=01-05-2025&to=05-05-2025&symbol=%s&dataType=priceVolumeDeliverable&series=ALL",
//            ticker
//        );
//
//        System.out.println("Fetching: " + url);
//
//        JsonNode response =  FetchNSEAPI.NSEAPICall(url);
//        
//       System.out.println(response);
//
//        return response.toString(); // JSON string response
//    }
//	
//    public static void main(String[] args) {
//        try {
//            String ticker = "RELIANCE"; // Replace with your ticker
//            String jsonResponse = FetchPriceVolumeDeliverablesData(ticker);
//            System.out.println("Response: " + jsonResponse);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//    
//}
