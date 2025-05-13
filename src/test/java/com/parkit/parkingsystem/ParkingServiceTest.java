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

    // ----- LISTE DE DONNEES -----

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
                Arguments.of(false, 1),
                Arguments.of(false, 3),
                Arguments.of(false, 5),
                Arguments.of(false, 8),
                Arguments.of(false, 10),
                Arguments.of(false, 12),
                Arguments.of(true, 1),
                Arguments.of(true, 3),
                Arguments.of(true, 5),
                Arguments.of(true, 8),
                Arguments.of(true, 10),
                Arguments.of(true, 12)
        );
    }

    public static Stream<Arguments> vehicleTypes() {
        return Stream.of(
                Arguments.of(1),
                Arguments.of(2)
        );
    }

    // ----- METHODE MOCKER -----

    public void mockUpdateParkingAndGetNbTicket(boolean typeUser) {
        when(ticketDAO.getNbTickets(REG_NUMBER)).thenReturn(typeUser);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
    }

    public void mockReadSelectionAndGetNextAvailableSlot_ThenReturnTypeVehicleAnd1(int typeVehicle) {
        when(inputReaderUtil.readSelection()).thenReturn(typeVehicle);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
    }


    // ----- TEST ENTREE VEHICULE -----

    @ParameterizedTest
    @MethodSource("provideVehicleTypeAndUserStatus")
    @DisplayName("Gérer l'entrée d'un véhicule selon le type et le statut utilisateur")
    public void testProcessIncomingVehicle(int typeVehicle, boolean typeUser) throws Exception {
        // GIVEN
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(REG_NUMBER);
        mockUpdateParkingAndGetNbTicket(typeUser);
        mockReadSelectionAndGetNextAvailableSlot_ThenReturnTypeVehicleAnd1(typeVehicle);

        // WHEN
        parkingService.processIncomingVehicle();

        // THEN
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(any(ParkingType.class));
        verify(ticketDAO, times(1)).saveTicket(any(Ticket.class));
        verify(ticketDAO, times(1)).getNbTickets(REG_NUMBER);
    }

    @ParameterizedTest
    @MethodSource("vehicleTypes")
    @DisplayName("Gérer la sortie d'un véhicule - exception si lecture de la plaque échoue")
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


    // ----- TEST SORTIE VEHICULE ------

    @ParameterizedTest
    @MethodSource("userTypes")
    @DisplayName("Gérer la sortie d'un véhicule - situation normale")
    public void testProcessExitingVehicle(boolean typeUser, int hours) throws Exception {
        // GIVEN
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(REG_NUMBER);
        mockUpdateParkingAndGetNbTicket(typeUser);
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
        ticket.setInTime(new Date(System.currentTimeMillis() - ((long) hours * 60 * 60 * 1000)));

        // WHEN
        parkingService.processExitingVehicle();

        // THEN
        Ticket ticket1 = ticketDAO.getTicket(REG_NUMBER);
        final double result = (ticket1.getOutTime().getTime()-ticket1.getInTime().getTime())/ (1_000.0 * 60.0 * 60.0);
        assertEquals(ticket1.getPrice(), typeUser ? FareCalculatorService.round(1.5*result*0.95) : FareCalculatorService.round(1.5*result),  "Le prix du ticket en sortie doit correspondre");
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
        verify(ticketDAO, Mockito.times(1)).getNbTickets(REG_NUMBER);
    }

    @Test
    @DisplayName("processExitingVehicle : doit capturer une exception si la lecture de la plaque échoue")
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
    @DisplayName("processExitingVehicle : cas d’échec de la mise à jour du ticket (updateTicket retourne false)")
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


    // ----- TESTS POUR CHOIX DE PLACE DISPONIBLE -----

    @ParameterizedTest
    @CsvSource({
            "1, CAR, 1",
            "2, BIKE, 4"
    })
    @DisplayName("getNextParkingNumberIfAvailable : retourne une place valide selon le type de véhicule")
    public void testGetNextParkingNumberIfAvailable(int number, ParkingType expectedType, int placeNumber) {
        // GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(number);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(placeNumber);

        // WHEN
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        //THEN
        assertEquals(placeNumber, parkingSpot.getId(), "Le numéro de parking doit être 1");
        assertEquals(expectedType, parkingSpot.getParkingType(), "Le type de véhicule n'est pas correct");
        assertTrue(parkingSpot.isAvailable(), "Le parkingSpot doit être disponible");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    @DisplayName("getNextParkingNumberIfAvailable : retourne null si aucune place n'est disponible")
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound(int typeOfVehicle)  {
        // GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(typeOfVehicle);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(-1);

        // WHEN
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        // THEN
        assertNull(parkingSpot, "Le parkingSpot retourné doit être null");
    }

    @Test
    @DisplayName("getNextParkingNumberIfAvailable : retourne null si type de véhicule invalide")
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument()  {
        //GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(3);

        //WHEN
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        // THEN
        assertNull(parkingSpot, "Le parkingSpot retourné doit être null");
    }

}
