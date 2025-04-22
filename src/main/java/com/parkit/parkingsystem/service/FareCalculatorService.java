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
        double duration = durationMillis / (1000.0 * 60.0 * 60.0); //conversion en heure
        double finalDuration = (duration < 0.5) ? 0.0 : duration;

        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                if (discount){
                    ticket.setPrice(finalDuration * Fare.CAR_RATE_PER_HOUR * 0.95);
                }
                else {
                    ticket.setPrice(finalDuration * Fare.CAR_RATE_PER_HOUR);
                }
                break;
            }
            case BIKE: {
                if (discount){
                    ticket.setPrice(finalDuration * Fare.BIKE_RATE_PER_HOUR * 0.95);
                }
                else {
                    ticket.setPrice(finalDuration * Fare.BIKE_RATE_PER_HOUR);
                }
                break;
            }
            default: throw new IllegalArgumentException("Unknown Parking Type");
        }
    }

    public void calculateFare(Ticket ticket){
        calculateFare(ticket,false);
    }
}