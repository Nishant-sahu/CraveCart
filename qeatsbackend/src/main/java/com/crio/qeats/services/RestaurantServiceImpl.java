
/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.services;

import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.repositoryservices.RestaurantRepositoryService;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class RestaurantServiceImpl implements RestaurantService {

  private final Double peakHoursServingRadiusInKms = 3.0;
  private final Double normalHoursServingRadiusInKms = 5.0;
  @Autowired
  private RestaurantRepositoryService restaurantRepositoryService;




  // TODO: CRIO_TASK_MODULE_RESTAURANTSAPI - Implement findAllRestaurantsCloseby.
  // Check RestaurantService.java file for the interface contract.

  private boolean isTimeWithInRange(LocalTime timeNow,
  LocalTime startTime, LocalTime endTime) {
return timeNow.isAfter(startTime) && timeNow.isBefore(endTime);
}

public boolean isPeakHour(LocalTime timeNow) {
return isTimeWithInRange(timeNow, LocalTime.of(7, 59, 59), LocalTime.of(10, 00, 01))
    || isTimeWithInRange(timeNow, LocalTime.of(12, 59, 59), LocalTime.of(14, 00, 01))
    || isTimeWithInRange(timeNow, LocalTime.of(18, 59, 59), LocalTime.of(21, 00, 01));
}


  @Override
  public GetRestaurantsResponse findAllRestaurantsCloseBy(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

    Double servingRadiusInKms =
        isPeakHour(currentTime) ? peakHoursServingRadiusInKms : normalHoursServingRadiusInKms;

    List<Restaurant> restaurantsCloseBy = restaurantRepositoryService.findAllRestaurantsCloseBy(
        getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(),
        currentTime, servingRadiusInKms);
 
    return new GetRestaurantsResponse(restaurantsCloseBy);

  }

  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQuery(
          GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

    // lattitude: 20.0 , Longitude: 30.0 , SearchFor: Test , currentTime: 22.00.00 (10 PM)
    List<List<Restaurant>> restaurantAnsList=new ArrayList<>();

    Double servingRadiusInKms = isPeakHour(currentTime) ? peakHoursServingRadiusInKms : normalHoursServingRadiusInKms;
    //ServingRadiusInKMs= 3.0 km

    String searchFor = getRestaurantsRequest.getSearchFor();

    Set<String> restaurantSet = new HashSet<>();

    List<Restaurant> restaurantList = new ArrayList<>();

    if(!searchFor.isEmpty()){

      //By Name
      restaurantAnsList.add(restaurantRepositoryService.findRestaurantsByName(getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(), searchFor, currentTime, servingRadiusInKms));

      //By Attributes
      restaurantAnsList.add(restaurantRepositoryService.findRestaurantsByAttributes(getRestaurantsRequest.getLatitude(),
              getRestaurantsRequest.getLongitude(), searchFor, currentTime, servingRadiusInKms));

      //By ItemName
      restaurantAnsList.add(restaurantRepositoryService.findRestaurantsByItemName(getRestaurantsRequest.getLatitude(),
              getRestaurantsRequest.getLongitude(), searchFor, currentTime, servingRadiusInKms));

      //By ItemAttributes
      restaurantAnsList.add(restaurantRepositoryService.findRestaurantsByItemAttributes(getRestaurantsRequest.getLatitude(),
              getRestaurantsRequest.getLongitude(), searchFor, currentTime, servingRadiusInKms));


      // restaurantList = restaurantAnsList.stream().distinct().collect(Collectors.toList());


      for(List<Restaurant> ListRestIter:restaurantAnsList){

        for(Restaurant restListRestIterIter: ListRestIter){

          if(!restaurantSet.contains(restListRestIterIter.getRestaurantId())){
            restaurantSet.add(restListRestIterIter.getName());
            restaurantList.add(restListRestIterIter);
          }
        }
      }

      return new GetRestaurantsResponse(restaurantList);

    }else{

      return new GetRestaurantsResponse(new ArrayList<>());

    }

  }





}

