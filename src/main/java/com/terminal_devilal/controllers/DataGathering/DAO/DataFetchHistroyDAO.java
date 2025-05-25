package com.terminal_devilal.controllers.DataGathering.DAO;

import com.terminal_devilal.controllers.DataGathering.Model.DataFetchHistroy;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataFetchHistroyDAO extends JpaRepository<DataFetchHistroy, String> {

//	@Query(value = """
//			    SELECT ticker, MAX(processed_date) as processed_date
//			    FROM pcdt
//			    GROUP BY ticker
//			""", nativeQuery = true)
//	List<DataFetchHistroy> findLatestProcessedDates();

}
