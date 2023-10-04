package com.driver.services;


import com.driver.EntryDto.BookTicketEntryDto;
import com.driver.model.Passenger;
import com.driver.model.Station;
import com.driver.model.Ticket;
import com.driver.model.Train;
import com.driver.repository.PassengerRepository;
import com.driver.repository.TicketRepository;
import com.driver.repository.TrainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TicketService {

    @Autowired
    TicketRepository ticketRepository;

    @Autowired
    TrainRepository trainRepository;

    @Autowired
    PassengerRepository passengerRepository;


    public Integer bookTicket(BookTicketEntryDto bookTicketEntryDto)throws Exception{

        //Check for validity
        //Use bookedTickets List from the TrainRepository to get bookings done against that train
        // Incase the there are insufficient tickets
        // throw new Exception("Less tickets are available");
        //otherwise book the ticket, calculate the price and other details
        //Save the information in corresponding DB Tables
        //Fare System : Check problem statement
        //Incase the train doesn't pass through the requested stations
        //throw new Exception("Invalid stations");
        //Save the bookedTickets in the train Object
        //Also in the passenger Entity change the attribute bookedTickets by using the attribute bookingPersonId.
       //And the end return the ticketId that has come from db

        Train train = trainRepository.findById(bookTicketEntryDto.getTrainId()).orElse(null);

        if(train==null){
            return 0;
        }

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

        if(!indexOfStation.containsKey(bookTicketEntryDto.getFromStation().toString())){
            throw new Exception("Invalid stations");
        }

        if(!indexOfStation.containsKey(bookTicketEntryDto.getToStation().toString())){
            throw new Exception("Invalid stations");
        }

        int startIndex =indexOfStation.get(bookTicketEntryDto.getFromStation().toString());
        int toIndex = indexOfStation.get(bookTicketEntryDto.getToStation().toString());

        if(toIndex<=startIndex){
            throw new Exception("Invalid stations");
        }

        for( int j=indexOfStation.get(bookTicketEntryDto.getFromStation().toString());j<indexOfStation.get(bookTicketEntryDto.getToStation().toString());j++){
            if(feq[j]+bookTicketEntryDto.getNoOfSeats()>train.getNoOfSeats()){
                throw new Exception("Less tickets are available");
            }
        }

        List<Passenger> passengers = new ArrayList<>();

        for(int x: bookTicketEntryDto.getPassengerIds()){
            passengers.add(passengerRepository.findById(x).get());
        }


        Ticket ticket = new Ticket();

        ticket.setFromStation(bookTicketEntryDto.getFromStation());
        ticket.setToStation(bookTicketEntryDto.getToStation());
        ticket.setTrain(train);
        ticket.setPassengersList(passengers);

        int totalFare = (toIndex - startIndex)*300*bookTicketEntryDto.getNoOfSeats();

        ticket.setTotalFare(totalFare);

        Passenger bookingPerson = passengerRepository.findById(bookTicketEntryDto.getBookingPersonId()).get();
        bookingPerson.getBookedTickets().add(ticket);


        Ticket savedTicket =  ticketRepository.save(ticket);


        train.getBookedTickets().add(savedTicket);
        trainRepository.save(train);



       return savedTicket.getTicketId();

    }
}
