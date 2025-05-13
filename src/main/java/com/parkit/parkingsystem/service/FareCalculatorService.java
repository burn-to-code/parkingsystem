package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket, Boolean discount){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        long inMillis = ticket.getInTime().getTime();
        long outMillis = ticket.getOutTime().getTime();
        long durationMillis = outMillis - inMillis;

        //TODO: Some tests are failing here. Need to check if this logic is correct
        double durationHours = durationMillis / (1_000.0 * 60.0 * 60.0); //conversion en heure

        if(durationHours < 0.5){
            ticket.setPrice(0);
            return;
        }

        double fare;
        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                fare = Fare.CAR_RATE_PER_HOUR;
                break;
            }
            case BIKE: {
                fare = Fare.BIKE_RATE_PER_HOUR;
                break;
            }
            default: throw new IllegalArgumentException("Unknown Parking Type");
        }

        double discountValue =  discount ? 0.95 : 1;
        double roundPrice = round(durationHours * fare * discountValue);

        ticket.setPrice(roundPrice);
    }

    public void calculateFare(Ticket ticket){
        calculateFare(ticket,false);
    }
}
