package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

    // -----SETUP -----

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
                Arguments.of(45 , ParkingType.CAR , 1.13),
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

    // ----- START TESTS -----


    // --- TESTS FOR CAR ---


    @Test
    @DisplayName("Should calculate fare for car with less than one hour parking time")
    public void calculateFareCarWithLessThanOneHourParkingTime(){
        // GIVEN
        setUpTicketAndParkingSpot(45*60*1000,1,ParkingType.CAR,false);
        final double expectedResult = (double) Math.round((0.75 * Fare.CAR_RATE_PER_HOUR) * 100) / 100;

        // WHEN
        fareCalculatorService.calculateFare(ticket);

        // THEN
        assertEquals( expectedResult , ticket.getPrice(), "Ticket car price is not correct for 45 minutes");
    }

    @Test
    @DisplayName("Should calculate fare correctly for a car parked for 1 hour")
    public void calculateFareCar(){
        // GIVEN
        setUpTicketAndParkingSpot(60*60*1000,1,ParkingType.CAR,false);

        // WHEN
        fareCalculatorService.calculateFare(ticket);

        // THEN
        assertEquals(Fare.CAR_RATE_PER_HOUR, ticket.getPrice(), "Ticket car price is not correct for one hour");
    }

    @Test
    @DisplayName("Should calculate fare for car with more than one day parking time")
    public void calculateFareCarWithMoreThanADayParkingTime(){
        // GIVEN
        setUpTicketAndParkingSpot(24*60*60*1000,1,ParkingType.CAR,false);

        // WHEN
        fareCalculatorService.calculateFare(ticket);

        // THEN
        assertEquals( (24 * Fare.CAR_RATE_PER_HOUR) , ticket.getPrice(), "Ticket car price is not correct for 24 hours");
    }

    @Test
    @DisplayName("Should calculate fare with discount for car parking")
    public void calculateFareCarWithDiscountDescription() {
        // GIVEN
        setUpTicketAndParkingSpot(24*60*60*1000,1,ParkingType.CAR,false);

        final double expectedResult = FareCalculatorService.round(24 * Fare.CAR_RATE_PER_HOUR * 0.95);
        // WHEN
        fareCalculatorService.calculateFare(ticket, true);

        // THEN
        assertEquals( expectedResult, ticket.getPrice(),  "Ticket car price is not correct for 24 hours with a discount ticket");
    }


    // --- TESTS FOR BIKE ---


    @Test
    @DisplayName("Should calculate fare for bike with less than one hour parking time")
    public void calculateFareBikeWithLessThanOneHourParkingTime(){
        // GIVEN
        setUpTicketAndParkingSpot(45*60*1000,4,ParkingType.BIKE,false);

        // WHEN
        fareCalculatorService.calculateFare(ticket);

        // THEN
        assertEquals((0.75 * Fare.BIKE_RATE_PER_HOUR), ticket.getPrice(), "Ticket bike price is not correct for 45 minutes");
    }

    @Test
    @DisplayName("Should calculate fare correctly for a bike parked for 1 hour")
    public void calculateFareBike(){
        // GIVEN
        setUpTicketAndParkingSpot(60*60*1000,4,ParkingType.BIKE,false);

        // WHEN
        fareCalculatorService.calculateFare(ticket);

        // THEN
        assertEquals(Fare.BIKE_RATE_PER_HOUR, ticket.getPrice(), "Ticket Bike price is not correct for one hour");
    }

    @Test
    @DisplayName("Should calculate fare for bike with more than one day parking time")
    public void calculateFareBikeWithMoreThanADayParkingTime(){
        // GIVEN
        setUpTicketAndParkingSpot(24*60*60*1000,4,ParkingType.BIKE,false);

        // WHEN
        fareCalculatorService.calculateFare(ticket);

        // THEN
        assertEquals( (24 * Fare.BIKE_RATE_PER_HOUR) , ticket.getPrice(), "Ticket bike price is not correct for 24 hours");
    }

    @Test
    @DisplayName("Should calculate fare with discount for bike parking")
    public void calculateFareBikeWithDiscountDescription() {
        // GIVEN
        setUpTicketAndParkingSpot(24*60*60*1000,1,ParkingType.BIKE,false);

        final double expectedResult = FareCalculatorService.round(24 * Fare.BIKE_RATE_PER_HOUR * 0.95);
        // WHEN
        fareCalculatorService.calculateFare(ticket, true);

        // THEN
        assertEquals( expectedResult , ticket.getPrice(),  "Ticket bike price is not correct for  24 hours with a discount ticket");
    }


    // --- TESTS FOR EXCEPTION ---


    @Test
    @DisplayName("Should throw NullPointerException for unknown parking type")
    public void calculateFareUnknownType(){
        // GIVEN
        setUpTicketAndParkingSpot(60*60*1000,1,null,false);

        // WHEN THEN
        assertThrows(NullPointerException.class, () -> fareCalculatorService.calculateFare(ticket), "An NullPointerException should be thrown");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when parking time is in the future")
    public void calculateFareBikeWithFutureInTime(){
        // GIVEN
        setUpTicketAndParkingSpotInFuture(60*60*1000,4,ParkingType.BIKE,false);

        // WHEN AND THEN
        assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket), "An IllegalArgumentException should be thrown");
    }


    // --- MULTIPLE TESTS FOR ANY PARKING TYPE ---


    @ParameterizedTest
    @MethodSource("calculateFareSource")
    @DisplayName("Should calculate fare based on parking time and type")
    public void calculateFareParkingTime(int minutesParked,  ParkingType parkingType,  double expectedPrice){
        // GIVEN
        setUpTicketAndParkingSpot((long) minutesParked*60*1000,1,parkingType,false);

        // WHEN
        fareCalculatorService.calculateFare(ticket);

        // THEN
        assertEquals( expectedPrice , ticket.getPrice(), "Ticket price is not correct for "+minutesParked+" minutes" );
    }

}
