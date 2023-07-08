/*
 *
 * * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositoryservices;

import ch.hsr.geohash.GeoHash;
import com.crio.qeats.configs.RedisConfiguration;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.globals.GlobalConstants;
import com.crio.qeats.models.ItemEntity;
import com.crio.qeats.models.MenuEntity;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.repositories.ItemRepository;
import com.crio.qeats.repositories.MenuRepository;
import com.crio.qeats.repositories.RestaurantRepository;
import com.crio.qeats.utils.GeoLocation;
import com.crio.qeats.utils.GeoUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;


@Service
public class RestaurantRepositoryServiceImpl implements RestaurantRepositoryService {


  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private Provider<ModelMapper> modelMapperProvider;


  @Autowired
  private RestaurantRepository restaurantRepository;

  private final Double peakHoursServingRadiusInKms = 3.0;
  private final Double normalHoursServingRadiusInKms = 5.0;

  @Autowired
  private RedisConfiguration redisConfiguration;



  private boolean isOpenNow(LocalTime time, RestaurantEntity res) {
    LocalTime openingTime = LocalTime.parse(res.getOpensAt());
    LocalTime closingTime = LocalTime.parse(res.getClosesAt());

    return time.isAfter(openingTime) && time.isBefore(closingTime);
  }

  // DONE: CRIO_TASK_MODULE_NOSQL
  // Objectives:
  // 1. Implement findAllRestaurantsCloseby.
  // 2. Remember to keep the precision of GeoHash in mind while using it as a key.
  // Check RestaurantRepositoryService.java file for the interface contract.
  // public List<Restaurant> findAllRestaurantsCloseBy(Double latitude, Double longitude,
  // LocalTime currentTime, Double servingRadiusInKms) {

  // // List<Restaurant> restaurants = mongoTemplate.findAll(Restaurant.class, "restaurants");
  // // System.out.println("d");

  // // CHECKSTYLE:OFF
  // // CHECKSTYLE:ON


  // List<RestaurantEntity> restaurantEntities = restaurantRepository.findAll();


  // List<Restaurant> restaurants = new ArrayList<>();



  // for (RestaurantEntity entity : restaurantEntities) {

  // // servingRadiusInKms =
  // // isPeakHour(currentTime) ? peakHoursServingRadiusInKms : normalHoursServingRadiusInKms;

  // if (isRestaurantCloseByAndOpen(entity , currentTime , latitude , longitude ,
  // servingRadiusInKms)) {

  // restaurants.add(new Restaurant(entity.getId(), entity.getRestaurantId(), entity.getName(),
  // entity.getCity(), entity.getImageUrl(), entity.getLatitude(), entity.getLongitude(),
  // entity.getOpensAt(), entity.getClosesAt(), entity.getAttributes()));

  // }

  // //if(count == 6) break;

  // }

  // // System.out.println(restaurants);


  // return restaurants;

  // }


  public List<Restaurant> findAllRestaurantsCloseBy(Double latitude, Double longitude,
      LocalTime currentTime, Double servingRadiusInKms) {
    List<Restaurant> restaurants = null;
    if (redisConfiguration.isCacheAvailable()) {
      restaurants =
          findAllRestaurantsCloseByFromCache(latitude, longitude, currentTime, servingRadiusInKms);
    } else {
      restaurants =
          findAllRestaurantsCloseFromDb(latitude, longitude, currentTime, servingRadiusInKms);
    }
    return restaurants;
  }

  private List<Restaurant> findAllRestaurantsCloseFromDb(Double latitude, Double longitude,
      LocalTime currentTime, Double servingRadiusInKms) {
    ModelMapper modelMapper = modelMapperProvider.get();
    List<RestaurantEntity> restaurantEntities = restaurantRepository.findAll();
    List<Restaurant> restaurants = new ArrayList<Restaurant>();
    for (RestaurantEntity restaurantEntity : restaurantEntities) {
      if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime, latitude, longitude,
          servingRadiusInKms)) {
        restaurants.add(modelMapper.map(restaurantEntity, Restaurant.class));
      }
    }
    return restaurants;
  }

  private List<Restaurant> findAllRestaurantsCloseByFromCache(Double latitude, Double longitude,
      LocalTime currentTime, Double servingRadiusInKms) {
    List<Restaurant> restaurantList = new ArrayList<>();

    GeoLocation geoLocation = new GeoLocation(latitude, longitude);
    GeoHash geoHash =
        GeoHash.withCharacterPrecision(geoLocation.getLatitude(), geoLocation.getLongitude(), 7);

    try (Jedis jedis = redisConfiguration.getJedisPool().getResource()) {
      String jsonStringFromCache = jedis.get(geoHash.toBase32());

      if (jsonStringFromCache == null) {
        // Cache needs to be updated.
        String createdJsonString = "";
        try {
          restaurantList = findAllRestaurantsCloseFromDb(geoLocation.getLatitude(),
              geoLocation.getLongitude(), currentTime, servingRadiusInKms);
          createdJsonString = new ObjectMapper().writeValueAsString(restaurantList);
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }

        // Do operations with jedis resource
        jedis.setex(geoHash.toBase32(), GlobalConstants.REDIS_ENTRY_EXPIRY_IN_SECONDS,
            createdJsonString);
      } else {
        try {
          restaurantList = new ObjectMapper().readValue(jsonStringFromCache,
              new TypeReference<List<Restaurant>>() {});
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return restaurantList;
  }

  private boolean isTimeWithInRange(LocalTime timeNow, LocalTime startTime, LocalTime endTime) {
    return timeNow.isAfter(startTime) && timeNow.isBefore(endTime);
  }

  public boolean isPeakHour(LocalTime timeNow) {
    return isTimeWithInRange(timeNow, LocalTime.of(7, 59, 59), LocalTime.of(10, 00, 01))
        || isTimeWithInRange(timeNow, LocalTime.of(12, 59, 59), LocalTime.of(14, 00, 01))
        || isTimeWithInRange(timeNow, LocalTime.of(18, 59, 59), LocalTime.of(21, 00, 01));
  }



  // DONE: CRIO_TASK_MODULE_NOSQL
  // Objective:
  // 1. Check if a restaurant is nearby and open. If so, it is a candidate to be returned.
  // NOTE: How far exactly is "nearby"?

  /**
   * Utility method to check if a restaurant is within the serving radius at a given time.
   * 
   * public List<Restaurant> findAllRestaurantsCloseBy(Double latitude, Double longitude, LocalTime
   * currentTime, Double servingRadiusInKms) {
   * 
   * List<Restaurant> restaurants = null;
   * 
   * 
   * 
   * 
   * return restaurants; }
   * 
   * // Done: CRIO_TASK_MODULE_RESTAURANTSEARCH // Objective: // Find restaurants whose names have
   * an exact or partial match with the search query.
   * 
   * @Override public List<Restaurant> findRestaurantsByName(Double latitude, Double longitude,
   *           String searchString, LocalTime currentTime, Double servingRadiusInKms) {
   * 
   * 
   *           return null; }
    
    
              // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH // Objective: // Find restaurants whose
              attributes (cuisines) intersect with the search query.
    @Override public List<Restaurant> findRestaurantsByAttributes( Double latitude, Double
   *           longitude, String searchString, LocalTime currentTime, Double servingRadiusInKms) {
   * 
   * 
   *           return null; }
   * 
   * 
   * 
   *           // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH // Objective: // Find restaurants which
   *           serve food items whose names form a complete or partial match // with the search
   *           query.
   * 
   * @Override public List<Restaurant> findRestaurantsByItemName( Double latitude, Double longitude,
   *           String searchString, LocalTime currentTime, Double servingRadiusInKms) {
   * 
   * 
   *           return null; }
   * 
   *           // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH // Objective: // Find restaurants which
   *           serve food items whose attributes intersect with the search query.
   * @Override public List<Restaurant> findRestaurantsByItemAttributes(Double latitude, Double
   *           longitude, String searchString, LocalTime currentTime, Double servingRadiusInKms) {
   * 
   *           return null; }
   * 
   * 
   * 
   * 
   * 
   * 
   * 
   * 
   *           /** Utility method to check if a restaurant is within the serving radius at a given
   *           time.
   * @return boolean True if restaurant falls within serving radius and is open, false otherwise
   */
  private boolean isRestaurantCloseByAndOpen(RestaurantEntity restaurantEntity,
      LocalTime currentTime, Double latitude, Double longitude, Double servingRadiusInKms) {
    if (isOpenNow(currentTime, restaurantEntity)) {
      double dist = GeoUtils.findDistanceInKm(latitude, longitude, restaurantEntity.getLatitude(),
          restaurantEntity.getLongitude());
      boolean res = dist < servingRadiusInKms;
      return res;
    }

    return false;
  }

  @Override
  public List<Restaurant> findRestaurantsByName(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

    List<Restaurant> restaurants =
        findAllRestaurantsCloseBy(latitude, longitude, currentTime, servingRadiusInKms);
    List<Restaurant> res = new ArrayList<>();
    for (Restaurant r : restaurants) {
      if (r.getName().toLowerCase().contains(searchString.toLowerCase())) {
        res.add(r);
      }
    }
    return res;

  }

  @Override
  public List<Restaurant> findRestaurantsByAttributes(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    List<Restaurant> restaurants =
        findAllRestaurantsCloseBy(latitude, longitude, currentTime, servingRadiusInKms);

        List<Restaurant> res = new ArrayList<>();
        for (Restaurant r : restaurants) {
          if (r.getAttributes().contains(searchString)) {
            res.add(r);
          }
        }
        return res;
  }


  @Override
  public List<Restaurant> findRestaurantsByItemName(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    // List<Restaurant> restaurants =
    //     findAllRestaurantsCloseBy(latitude, longitude, currentTime, servingRadiusInKms);
    //     List<Restaurant> res = new ArrayList<>();
    //     for (Restaurant r : restaurants) {
    //       if (r.getAttributes().contains(searchString)) {
    //         res.add(r);
    //       }
    //     }
    //     return res;
    return new ArrayList<Restaurant>();
  }

  @Override
  public List<Restaurant> findRestaurantsByItemAttributes(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    // List<Restaurant> restaurants =
        // findAllRestaurantsCloseBy(latitude, longitude, currentTime, servingRadiusInKms);


        return new ArrayList<Restaurant>();
  }

}

