package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static final String REG_NUMBER = "ABCDEF";

    private ParkingService parkingService;
    private Ticket ticket;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

  // ----- SETUP -----

    private Ticket createTestTicket() {
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber(REG_NUMBER);
        return ticket;
    }

    @BeforeEach
    public void setUpPerTest() {
        try {
            ticket = createTestTicket();

            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }

    // ----- DATA SOURCES -----

    public static Stream<Arguments> provideVehicleTypeAndUserStatus() {
        return Stream.of(
                Arguments.of(1, false),
                Arguments.of(2, false),
                Arguments.of(1, true),
                Arguments.of(1, true)
        );
    }

    public static Stream<Arguments> userTypes() {
        return Stream.of(
                Arguments.of(false, 1, ParkingType.CAR),
                Arguments.of(false, 3, ParkingType.CAR),
                Arguments.of(false, 5, ParkingType.CAR),
                Arguments.of(false, 8, ParkingType.CAR),
                Arguments.of(false, 10, ParkingType.CAR),
                Arguments.of(false, 12, ParkingType.CAR),
                Arguments.of(true, 1, ParkingType.CAR),
                Arguments.of(true, 3, ParkingType.CAR),
                Arguments.of(true, 5, ParkingType.CAR),
                Arguments.of(true, 8, ParkingType.CAR),
                Arguments.of(true, 10, ParkingType.CAR),
                Arguments.of(true, 12, ParkingType.CAR),
                Arguments.of(false, 1, ParkingType.BIKE),
                Arguments.of(false, 3, ParkingType.BIKE),
                Arguments.of(false, 5, ParkingType.BIKE),
                Arguments.of(false, 8, ParkingType.BIKE),
                Arguments.of(false, 10, ParkingType.BIKE),
                Arguments.of(false, 12, ParkingType.BIKE),
                Arguments.of(true, 1, ParkingType.BIKE),
                Arguments.of(true, 3, ParkingType.BIKE),
                Arguments.of(true, 5, ParkingType.BIKE),
                Arguments.of(true, 8, ParkingType.BIKE),
                Arguments.of(true, 10, ParkingType.BIKE),
                Arguments.of(true, 12, ParkingType.BIKE)
        );
    }

    // ----- METHOD MOCKED -----

    public void mockUpdateParkingAndGetNbTicket(boolean typeUser) {
        when(ticketDAO.getNbTickets(REG_NUMBER)).thenReturn(typeUser);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
    }

    public void mockReadSelectionAndGetNextAvailableSlot_ThenReturnTypeVehicleAnd1(int typeVehicle) {
        when(inputReaderUtil.readSelection()).thenReturn(typeVehicle);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
    }


    // ----- INCOMING VEHICLE TESTS -----

    @ParameterizedTest
    @MethodSource("provideVehicleTypeAndUserStatus")
    @DisplayName("Handle incoming vehicle according to type and user status")
    public void testProcessIncomingVehicle(int typeVehicle, boolean typeUser) throws Exception {
        // GIVEN
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(REG_NUMBER);
        mockUpdateParkingAndGetNbTicket(typeUser);
        mockReadSelectionAndGetNextAvailableSlot_ThenReturnTypeVehicleAnd1(typeVehicle);

        // captor
        ArgumentCaptor<ParkingSpot> parkingSpotCaptor = ArgumentCaptor.forClass(ParkingSpot.class);
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);

        // WHEN
        parkingService.processIncomingVehicle();

        // THEN
        verify(parkingSpotDAO, times(1)).updateParking(parkingSpotCaptor.capture());
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(any(ParkingType.class));
        verify(ticketDAO, times(1)).saveTicket(ticketCaptor.capture());
        verify(ticketDAO, times(1)).getNbTickets(REG_NUMBER);

        ParkingSpot updatedSpot = parkingSpotCaptor.getValue();
        Ticket savedTicket = ticketCaptor.getValue();

        assertFalse(updatedSpot.isAvailable(), "ParkingSpot should be marked as not available");

        assertEquals("ABCDEF", savedTicket.getVehicleRegNumber(), "Vehicle registration number should match input");
        assertEquals(1, savedTicket.getParkingSpot().getId(), "Saved ticket should reference ParkingSpot with ID 1");
        assertNotNull(savedTicket.getInTime(), "Ticket inTime should not be null");
        assertNull(savedTicket.getOutTime(), "Ticket outTime should be null for incoming vehicle");
        assertEquals(0, savedTicket.getPrice(), "Price should be zero for incoming vehicle");


    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    @DisplayName("Handle vehicle entry - exception when reading license plate fails")
    public void testProcessIncomingVehicle_ShouldHandleException_WhenReadVehicleRegNumberFails(int typeVehicle) throws Exception {
        // GIVEN
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(REG_NUMBER);
        mockReadSelectionAndGetNextAvailableSlot_ThenReturnTypeVehicleAnd1(typeVehicle);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenThrow(new RuntimeException("Simulated exception"));

        // WHEN
        parkingService.processIncomingVehicle();

        // THEN
        verify(ticketDAO, never()).saveTicket(any());
        verify(parkingSpotDAO, never()).updateParking(any());
    }


    // ----- EXITING VEHICLE TESTS ------

    @ParameterizedTest
    @MethodSource("userTypes")
    @DisplayName("Handle vehicle exit - normal case")
    public void testProcessExitingVehicle(boolean typeUser, int hours, ParkingType parkingType) throws Exception {
        // GIVEN
        final double expectedPrice;

        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(REG_NUMBER);
        mockUpdateParkingAndGetNbTicket(typeUser);
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

        ticket.setInTime(new Date(System.currentTimeMillis() - ((long) hours * 60 * 60 * 1000)));
        ticket.setParkingSpot(new ParkingSpot(1, parkingType, false));

        // Captor
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);

        // WHEN
        parkingService.processExitingVehicle();

        // THEN
        verify(ticketDAO, times(1)).updateTicket(ticketCaptor.capture());
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, times(1)).getNbTickets(REG_NUMBER);

        Ticket ticket1 = ticketCaptor.getValue();
        final double result = (ticket1.getOutTime().getTime()-ticket1.getInTime().getTime())/ (1_000.0 * 60.0 * 60.0);
        expectedPrice = calculateFare(parkingType, typeUser,  result);

        assertEquals(expectedPrice, ticket1.getPrice(), "The calculated ticket price must match expected value");
        assertTrue(ticket1.getParkingSpot().isAvailable(), "Parking spot should be available after vehicle exits");

    }

    // ---- HELPERS

    public double calculateFare(ParkingType parkingType, boolean typeUser, double result) {
        final double expectedPrice;
        if (parkingType == ParkingType.CAR) {
            expectedPrice = typeUser
                    ? FareCalculatorService.round(1.5 * result * 0.95)
                    : FareCalculatorService.round(1.5 * result);
        } else {
            expectedPrice = typeUser
                    ? FareCalculatorService.round(1 * result * 0.95)
                    : FareCalculatorService.round(1 * result);
        }
        return expectedPrice;
    }

    @Test
    @DisplayName("processExitingVehicle: should catch exception if license plate reading fails")
    public void testProcessExitingVehicle_ShouldHandleException_WhenGetVehicleRegNumberFails() throws Exception {
        // GIVEN
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(REG_NUMBER);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenThrow(new RuntimeException("Simulated Exception"));

        // WHEN
        parkingService.processExitingVehicle();

        // THEN
        verify(ticketDAO, never()).getTicket(any());
        verify(ticketDAO, never()).updateTicket(any());
        verify(parkingSpotDAO, never()).updateParking(any());
    }

    @Test
    @DisplayName("processExitingVehicle: fail when ticket update fails (updateTicket returns false)")
    public void processExitingVehicleTestUnableUpdate() throws Exception {
        // GIVEN
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(REG_NUMBER);
        when(ticketDAO.getNbTickets(REG_NUMBER)).thenReturn(false);
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

        // WHEN
        parkingService.processExitingVehicle();

        // THEN
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
        verify(ticketDAO, Mockito.times(1)).getNbTickets(REG_NUMBER);
    }


    // ----- PARKING SLOT SELECTION TESTS -----

    @ParameterizedTest
    @CsvSource({
            "1, CAR, 1",
            "2, BIKE, 4"
    })
    @DisplayName("getNextParkingNumberIfAvailable: should return a valid slot based on vehicle type")
    public void testGetNextParkingNumberIfAvailable(int number, ParkingType expectedType, int placeNumber) {
        // GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(number);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(placeNumber);

        // WHEN
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        //THEN
        assertEquals(placeNumber, parkingSpot.getId(), "Parking spot ID should match");
        assertEquals(expectedType, parkingSpot.getParkingType(), "Vehicle type is incorrect");
        assertTrue(parkingSpot.isAvailable(), "Parking spot should be available");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    @DisplayName("getNextParkingNumberIfAvailable: should return null if no slot is available")
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound(int typeOfVehicle)  {
        // GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(typeOfVehicle);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(-1);

        // WHEN
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        // THEN
        assertNull(parkingSpot, "Returned parkingSpot should be null");
    }

    @Test
    @DisplayName("getNextParkingNumberIfAvailable: should return null if vehicle type is invalid")
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument()  {
        //GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(3);

        //WHEN
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        // THEN
        assertNull(parkingSpot, "Returned parkingSpot should be null");
    }

}
