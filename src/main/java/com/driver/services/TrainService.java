package com.driver.services;

import com.driver.EntryDto.AddTrainEntryDto;
import com.driver.EntryDto.SeatAvailabilityEntryDto;
import com.driver.model.Passenger;
import com.driver.model.Station;
import com.driver.model.Ticket;
import com.driver.model.Train;
import com.driver.repository.TrainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TrainService {

    @Autowired
    TrainRepository trainRepository;

    public Integer addTrain(AddTrainEntryDto trainEntryDto){

        //Add the train to the trainRepository
        //and route String logic to be taken from the Problem statement.
        //Save the train and return the trainId that is generated from the database.
        //Avoid using the lombok library
        Train train = new Train();

        List<Station> stations = trainEntryDto.getStationRoute();
        String route = "";

        for(int i=0;i<stations.size();i++){
            if(i<stations.size()-1){
                route = route + stations.get(i).toString() + ",";
            }else {
                route = route + stations.get(i).toString();
            }
        }

        train.setNoOfSeats(trainEntryDto.getNoOfSeats());
        train.setRoute(route);
        train.setBookedTickets(new ArrayList<>());
        train.setDepartureTime(trainEntryDto.getDepartureTime());


        Train savedTrain = trainRepository.save(train);

        return savedTrain.getTrainId();
    }

    public Integer calculateAvailableSeats(SeatAvailabilityEntryDto seatAvailabilityEntryDto){

        //Calculate the total seats available
        //Suppose the route is A B C D
        //And there are 2 seats avaialble in total in the train
        //and 2 tickets are booked from A to C and B to D.
        //The seat is available only between A to C and A to B. If a seat is empty between 2 station it will be counted to our final ans
        //even if that seat is booked post the destStation or before the boardingStation
        //Inshort : a train has totalNo of seats and there are tickets from and to different locations
        //We need to find out the available seats between the given 2 stations.

        int trainId = seatAvailabilityEntryDto.getTrainId();
        Station fromStation = seatAvailabilityEntryDto.getFromStation();
        Station toStation =seatAvailabilityEntryDto.getToStation();

        Train train = trainRepository.findById(trainId).get();

        List<Ticket> tickets = train.getBookedTickets();

        String route = train.getRoute();
        String[] stations = route.split(",");

        Map<String,Integer> indexOfStation = new HashMap<>();
        int i=0;
        for(String s : stations){
            indexOfStation.put(s,i);
            i++;
        }


        int[] feq = new int[stations.length];

        for(Ticket ticket : tickets){
            String fromS = ticket.getFromStation().toString();
            String toS = ticket.getToStation().toString();

            feq[indexOfStation.get(fromS)]+=ticket.getPassengersList().size();
            feq[indexOfStation.get(toS)]-=ticket.getPassengersList().size();
        }

        for(int j=1;j<feq.length;j++){
            feq[j]=feq[j]+feq[j-1];
        }

        int fromIndex = indexOfStation.get(seatAvailabilityEntryDto.getFromStation().toString());
        int toIndex = indexOfStation.get(seatAvailabilityEntryDto.getToStation().toString());

        int available = Integer.MAX_VALUE;

        for(int j=fromIndex;j<toIndex;j++){
            int temp = train.getNoOfSeats();
            available = Math.min(available,temp-feq[j]);
        }

        return available;
    }

    public Integer calculatePeopleBoardingAtAStation(Integer trainId,Station station) throws Exception{

        //We need to find out the number of people who will be boarding a train from a particular station
        //if the trainId is not passing through that station
        //throw new Exception("Train is not passing from this station");
        //  in a happy case we need to find out the number of such people.

        Train train = trainRepository.findById(trainId).get();

        List<Ticket> tickets = train.getBookedTickets();

        String route = train.getRoute();
        String[] stations = route.split(",");

        Map<String,Integer> indexOfStation = new HashMap<>();
        int i=0;
        for(String s : stations){
            indexOfStation.put(s,i);
            i++;
        }

        if(!indexOfStation.containsKey(station.toString())){
            throw new Exception("Train is not passing from this station");
        }

        int[] feq = new int[stations.length];

        for(Ticket ticket : tickets){
            String fromS = ticket.getFromStation().toString();
            String toS = ticket.getToStation().toString();

            feq[indexOfStation.get(fromS)]+=ticket.getPassengersList().size();
            feq[indexOfStation.get(toS)]-=ticket.getPassengersList().size();
        }


        return feq[indexOfStation.get(station.toString())];
    }

    public Integer calculateOldestPersonTravelling(Integer trainId){

        //Throughout the journey of the train between any 2 stations
        //We need to find out the age of the oldest person that is travelling the train
        //If there are no people travelling in that train you can return 0

        int ans = 0;
        Train train = trainRepository.findById(trainId).get();

        List<Ticket> bookedTickets = train.getBookedTickets();

        for(Ticket ticket : bookedTickets){
            List<Passenger> passengers = ticket.getPassengersList();
            for(Passenger passenger : passengers){
                ans = Math.max(ans,passenger.getAge());
            }
        }

        return ans;
    }

    public List<Integer> trainsBetweenAGivenTime(Station station, LocalTime startTime, LocalTime endTime){

        //When you are at a particular station you need to find out the number of trains that will pass through a given station
        //between a particular time frame both start time and end time included.
        //You can assume that the date change doesn't need to be done ie the travel will certainly happen with the same date (More details
        //in problem statement)
        //You can also assume the seconds and milli seconds value will be 0 in a LocalTime format.

        List<Train> trains = trainRepository.findAll();
        List<Integer> ans = new ArrayList<>();

        for(Train train : trains){
            String route = train.getRoute();
            LocalTime depart = train.getDepartureTime();

            String[] stations = route.split(",");
            Map<String,Integer> indexOfStation = new HashMap<>();
            int i=0;
            for(String s : stations){
                indexOfStation.put(s,i);
                i++;
            }
            if(!indexOfStation.containsKey(station.toString())) {continue;}

            LocalTime comTime = depart.plusHours(indexOfStation.get(station.toString()));

            if(comTime.compareTo(startTime)>=0 && comTime.compareTo(endTime)<=0){
                ans.add(train.getTrainId());
            }


        }

        return ans;
    }

}
