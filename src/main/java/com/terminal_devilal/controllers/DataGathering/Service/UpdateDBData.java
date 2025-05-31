//package com.terminal_devilal.controllers.DataGathering.Service;
//
//import java.util.List;
//import org.springframework.stereotype.Service;
//
//@Service
//public class UpdateDBData {
//
//	private ProcessedDatesService processedDatesService;
//
//	private List<String> links = List.of(
//			"https://www.nseindia.com/api/historical/securityArchives?from=%s&to=%s&symbol=%s&dataType=priceVolumeDeliverable&series=EQ");
//
//	public UpdateDBData(ProcessedDatesService processedDatesService) {
//		this.processedDatesService = processedDatesService;
//	}
//
////	public void updateData() throws IOException, InterruptedException {
////	    List<DataFetchHistroy> datesForProcessing = processedDatesService.getDatesForProcessing();
////
////	    for (DataFetchHistroy processedDateEntry : datesForProcessing) {
////	        for (String apiLinkTemplate : links) {
////	            String fromDate = processedDateEntry.getProcessedDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
////	            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
////	            String formattedUrl = String.format(apiLinkTemplate, fromDate, today, processedDateEntry.getTicker());
////
////	            FetchNSEAPI.NSEAPICall(formattedUrl);
////	        }
////	    }
////	}
//
//}
