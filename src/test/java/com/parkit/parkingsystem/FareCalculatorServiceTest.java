package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import java.util.stream.Stream;

public class FareCalculatorServiceTest {

    private static FareCalculatorService fareCalculatorService;
    private Ticket ticket;

    // -----SETUP-----

    @BeforeAll
    public static void setUp() {
        fareCalculatorService = new FareCalculatorService();
    }

    @BeforeEach
    public void setUpPerTest() {
        ticket = new Ticket();
    }

    // -----SETUP PERSONNALISE-----
    public void setUpTicketAndParkingSpot(long time, int number, ParkingType type, boolean available){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (time) );
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(number, type, available);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
    }

    public void setUpTicketAndParkingSpotInFuture(long time, int number, ParkingType type, boolean available){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() + (time) );
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(number, type, available);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
    }

    // ----- DATA SOURCE ------

    static Stream<Arguments> calculateFareSource(){
        return Stream.of(
                Arguments.of(1 , ParkingType.CAR , 0),
                Arguments.of(5 , ParkingType.CAR , 0),
                Arguments.of(10 , ParkingType.CAR , 0),
                Arguments.of(15 , ParkingType.CAR , 0),
                Arguments.of(20 , ParkingType.CAR , 0),
                Arguments.of(29 , ParkingType.CAR , 0),
                Arguments.of(30 , ParkingType.CAR , 0.75),
                Arguments.of(45 , ParkingType.CAR , 1.125),
                Arguments.of(60 , ParkingType.CAR , 1.5),
                Arguments.of(120 , ParkingType.CAR , 3),
                Arguments.of(1 , ParkingType.BIKE , 0),
                Arguments.of(5 , ParkingType.BIKE , 0),
                Arguments.of(10 , ParkingType.BIKE , 0),
                Arguments.of(15 , ParkingType.BIKE , 0),
                Arguments.of(20 , ParkingType.BIKE , 0),
                Arguments.of(29 , ParkingType.BIKE , 0),
                Arguments.of(30 , ParkingType.BIKE , 0.5),
                Arguments.of(45 , ParkingType.BIKE , 0.75),
                Arguments.of(60 , ParkingType.BIKE , 1),
                Arguments.of(120 , ParkingType.BIKE , 2)
        );
    }

    @Test
    public void calculateFareCar(){
        setUpTicketAndParkingSpot(60*60*1000,1,ParkingType.CAR,false);

        fareCalculatorService.calculateFare(ticket);

        assertEquals(Fare.CAR_RATE_PER_HOUR, ticket.getPrice());
    }

    @Test
    public void calculateFareBike(){
        setUpTicketAndParkingSpot(60*60*1000,1,ParkingType.BIKE,false);

        fareCalculatorService.calculateFare(ticket);

        assertEquals(Fare.BIKE_RATE_PER_HOUR, ticket.getPrice());
    }

    @Test
    public void calculateFareUnkownType(){
        setUpTicketAndParkingSpot(60*60*1000,1,null,false);

        assertThrows(NullPointerException.class, () -> fareCalculatorService.calculateFare(ticket));
    }

    @Test
    public void calculateFareBikeWithFutureInTime(){
        setUpTicketAndParkingSpotInFuture(60*60*1000,1,ParkingType.BIKE,false);

        assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket));
    }

    @Test
    public void calculateFareBikeWithLessThanOneHourParkingTime(){
        setUpTicketAndParkingSpot(45*60*1000,1,ParkingType.BIKE,false);

        fareCalculatorService.calculateFare(ticket);

        assertEquals((0.75 * Fare.BIKE_RATE_PER_HOUR), ticket.getPrice() );
    }

    @Test
    public void calculateFareCarWithLessThanOneHourParkingTime(){
        setUpTicketAndParkingSpot(45*60*1000,1,ParkingType.CAR,false);

        fareCalculatorService.calculateFare(ticket);

        assertEquals( (0.75 * Fare.CAR_RATE_PER_HOUR) , ticket.getPrice());
    }

    @Test
    public void calculateFareCarWithMoreThanADayParkingTime(){
        setUpTicketAndParkingSpot(24*60*60*1000,1,ParkingType.CAR,false);

        fareCalculatorService.calculateFare(ticket);

        assertEquals( (24 * Fare.CAR_RATE_PER_HOUR) , ticket.getPrice());
    }

    @ParameterizedTest
    @MethodSource("calculateFareSource")
    public void calculateFareParkingTime(int minutesParked,  ParkingType parkingType,  double expectedPrice){
        setUpTicketAndParkingSpot((long) minutesParked*60*1000,1,parkingType,false);

        fareCalculatorService.calculateFare(ticket);

        assertEquals( expectedPrice , ticket.getPrice());
    }

    @Test
    public void calculateFareCarWithDiscountDescription() {
        setUpTicketAndParkingSpot(24*60*60*1000,1,ParkingType.CAR,false);

        fareCalculatorService.calculateFare(ticket, true);

        assertEquals( (24 * Fare.CAR_RATE_PER_HOUR * 0.95) , ticket.getPrice());
    }

    @Test
    public void calculateFareBikeWithDiscountDescription() {
        setUpTicketAndParkingSpot(24*60*60*1000,1,ParkingType.BIKE,false);

        fareCalculatorService.calculateFare(ticket, true);

        assertEquals( (24 * Fare.BIKE_RATE_PER_HOUR * 0.95) , ticket.getPrice());
    }

}
