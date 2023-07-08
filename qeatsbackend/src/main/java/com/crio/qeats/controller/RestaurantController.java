/*
 *
 * * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.controller;

import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.services.RestaurantService;
import java.time.LocalTime;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TODO: CRIO_TASK_MODULE_RESTAURANTSAPI
// Implement Controller using Spring annotations.
// Remember, annotations have various "targets". They can be class level, method level or others.
@RestController
@RequestMapping(RestaurantController.RESTAURANT_API_ENDPOINT)
public class RestaurantController {

  public static final String RESTAURANT_API_ENDPOINT = "/qeats/v1";
  public static final String RESTAURANTS_API = "/restaurants";
  public static final String MENU_API = "/menu";
  public static final String CART_API = "/cart";
  public static final String CART_ITEM_API = "/cart/item";
  public static final String CART_CLEAR_API = "/cart/clear";
  public static final String POST_ORDER_API = "/order";
  public static final String GET_ORDERS_API = "/orders";

  @Autowired
  private RestaurantService restaurantService;



  // @GetMapping(RESTAURANTS_API)
  // public ResponseEntity<GetRestaurantsResponse> getRestaurants(
  // GetRestaurantsRequest getRestaurantsRequest) {


  // GeoLocation geoloc=new GeoLocation(getRestaurantsRequest.getLatitude(),
  // getRestaurantsRequest.getLongitude());

  // if(getRestaurantsRequest.getLongitude()==null||getRestaurantsRequest.getLatitude()==null||!geoloc.isValidGeoLocation()){

  // return ResponseEntity.badRequest().body(null);

  // }


  // // log.info("getRestaurants called with {}", g etRestaurantsRequest);
  // GetRestaurantsResponse getRestaurantsResponse;

  // //CHECKSTYLE:OFF
  // getRestaurantsResponse = restaurantService
  // .findAllRestaurantsCloseBy(getRestaurantsRequest, LocalTime.now());

  // if(getRestaurantsResponse!=null && !getRestaurantsResponse.getRestaurants().isEmpty()){

  // List<Restaurant> restResponse=getRestaurantsResponse.getRestaurants();

  // for(Restaurant restIter: restResponse){

  // restIter.setName(restIter.getName().replace("é", "/"));

  // }

  // getRestaurantsResponse.setRestaurants(restResponse);


  // }

  // // log.info("getRestaurants returned {}", getRestaurantsResponse);
  // System.out.println("Restaurants called with:::::::......."+getRestaurantsResponse);
  // //CHECKSTYLE:ON

  // return ResponseEntity.ok().body(getRestaurantsResponse);
  // }

  // TIP(MODULE_MENUAPI): Model Implementation for getting menu given a restaurantId.
  // Get the Menu for the given restaurantId
  // API URI: /qeats/v1/menu?restaurantId=11
  // Method: GET
  // Query Params: restaurantId
  // Success Output:
  // 1). If restaurantId is present return Menu
  // 2). Otherwise respond with BadHttpRequest.
  //
  // HTTP Code: 200
  // {
  // "menu": {
  // "items": [
  // {
  // "attributes": [
  // "South Indian"
  // ],
  // "id": "1",
  // "imageUrl": "www.google.com",
  // "itemId": "10",
  // "name": "Idly",
  // "price": 45
  // }
  // ],
  // "restaurantId": "11"
  // }
  // }
  // Error Response:
  // HTTP Code: 4xx, if client side error.
  // : 5xx, if server side error.
  // Eg:
  // curl -X GET "http://localhost:8081/qeats/v1/menu?restaurantId=11"

  @GetMapping(RESTAURANTS_API)
  public ResponseEntity<GetRestaurantsResponse> getRestaurants(
          @Valid GetRestaurantsRequest getRestaurantsRequest) {
            System.out.print(getRestaurantsRequest);

    GetRestaurantsResponse getRestaurantsResponse;


    String searchFor = getRestaurantsRequest.getSearchFor();
    boolean isSearch = searchFor != null && !searchFor.isEmpty();
    if (isSearch) {
      getRestaurantsResponse = restaurantService
              .findRestaurantsBySearchQuery(getRestaurantsRequest, LocalTime.now());
    } else {
      //CHECKSTYLE:OFF
      getRestaurantsResponse = restaurantService
              .findAllRestaurantsCloseBy(getRestaurantsRequest, LocalTime.now());
      //CHECKSTYLE:ON

    }

    if (getRestaurantsResponse != null && !getRestaurantsResponse.getRestaurants().isEmpty()) {
      getRestaurantsResponse.getRestaurants().forEach(restaurant -> {
        restaurant.setName(restaurant.getName().replace("é", "?"));
      });
    }

    if(getRestaurantsRequest.getLatitude() != null & getRestaurantsRequest.getLongitude() != null
      && getRestaurantsRequest.getLatitude() >= -90 && getRestaurantsRequest.getLatitude() <= 90 && 
      getRestaurantsRequest.getLongitude() >= -180 && getRestaurantsRequest.getLongitude() <= 180){
        return ResponseEntity.ok().body(getRestaurantsResponse);
      }else{
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
      }

    // return ResponseEntity.ok().body(getRestaurantsResponse);

  }
}



// log.info("getRestaurants called with {}", getRestaurantsRequest);

// List<Restaurant> restaurants=null;

// GetRestaurantsResponse getRestaurantsResponse = restaurantService
// .findAllRestaurantsCloseBy(getRestaurantsRequest, LocalTime.now());

// GeoLocation geoloc =
//     new GeoLocation(getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude());

// if (getRestaurantsRequest.getLongitude() == null || getRestaurantsRequest.getLatitude() == null
//     || !geoloc.isValidGeoLocation())

//   return ResponseEntity.badRequest().body(null);

//   /*
//    * *  Check if the lattitude and longitude of a place is non-NULL.And if the coordinates of longitude and
//    * latitude is within the valid range.
//    * *
//    *
//    */

//    if(

//     getRestaurantsRequest.getLatitude()!=null && getRestaurantsRequest.getLongitude()!=null &&

//     getRestaurantsRequest.getLatitude() >=-90 && getRestaurantsRequest.getLatitude()<=90 &&

//     getRestaurantsRequest.getLongitude()>=-180 && getRestaurantsRequest.getLongitude()<=180


//    ){

//     if(getRestaurantsRequest.getSearchFor()!=null && !getRestaurantsRequest.getSearchFor().equals("")){

//       getRestaurantsResponse = restaurantService.findRestaurantsBySearchQuery(getRestaurantsRequest, LocalTime.now());

//       restaurants=getRestaurantsResponse.getRestaurants();

//       for (Restaurant restIter : restaurants){

//         restIter.setName(restIter.getName().replace("é", "/"));
//      }

//       return ResponseEntity.ok().body(getRestaurantsResponse);


//     }else{

//       // getRestaurantsResponse = restaurantService.findAllRestaurantsCloseBy(getRestaurantsRequest, LocalTime.now());

//       // restaurants = getRestaurantsResponse.getRestaurants();
//       return ResponseEntity.ok().body(getRestaurantsResponse);

//     }

//   }else{
//     return ResponseEntity.badRequest().body(null);
//   }
// getRestaurantsResponse =
//     restaurantService.findAllRestaurantsCloseBy(getRestaurantsRequest, LocalTime.now());

// if (getRestaurantsResponse != null && !getRestaurantsResponse.getRestaurants().isEmpty()) {

//   List<Restaurant> restResponse = getRestaurantsResponse.getRestaurants();

//   for (Restaurant restIter : restResponse) {

//     restIter.setName(restIter.getName().replace("é", "/"));

//   }

//   getRestaurantsResponse.setRestaurants(restResponse);

// }
// // log.info("getRestaurants returned {}", getRestaurantsResponse);

// return ResponseEntity.ok().body(getRestaurantsResponse);



