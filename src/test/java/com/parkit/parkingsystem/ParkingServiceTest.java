package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static ParkingService parkingService;

    private static final String REG_NUMBER = "ABCDEF";

    private Ticket ticket;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    @BeforeEach
    public void setUpPerTest() {
        try {
            lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(REG_NUMBER);

            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
            ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber(REG_NUMBER);

            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }

    public static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(1, false),
                Arguments.of(2, false),
                Arguments.of(1, true),
                Arguments.of(1, true)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void processExitingVehicleTest_AndAllUsers(int typeVehicle, boolean typeUser){
        when(ticketDAO.getNbTickets(REG_NUMBER)).thenReturn(typeUser);
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

        parkingService.processExitingVehicle();

        assertNotNull(ticketDAO.getTicket(REG_NUMBER).getOutTime(), "l'heure de sortie de ne doit pas être null");
        assertTrue(ticketDAO.getTicket(REG_NUMBER).getPrice() > 0., "le prix du ticket doit être sup à 0");
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
        verify(ticketDAO, Mockito.times(1)).getNbTickets(REG_NUMBER);
    }

    @Test
    public void processExitingVehicle_ShouldHandleException_WhenGetVehicleRegNumberFails() throws Exception {
        // Arrange
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenThrow(new RuntimeException("Simulated Exception"));

        // Act
        parkingService.processExitingVehicle();

        // Assert
        verify(ticketDAO, never()).getTicket(any());
        verify(ticketDAO, never()).updateTicket(any());
        verify(parkingSpotDAO, never()).updateParking(any());
    }



    @ParameterizedTest
    @MethodSource("data")
    public void processIncomingVehicleTest_ForAllTypeVehicle_AndAllUsers(int typeVehicle, boolean typeUser){
        when(ticketDAO.getNbTickets(REG_NUMBER)).thenReturn(typeUser);
        when(inputReaderUtil.readSelection()).thenReturn(typeVehicle);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

        parkingService.processIncomingVehicle();

        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
        verify(parkingSpotDAO, Mockito.times(1)).getNextAvailableSlot(any(ParkingType.class));
        verify(ticketDAO, times(1)).saveTicket(any(Ticket.class));
        verify(ticketDAO, times(1)).getNbTickets(REG_NUMBER);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void processIncomingVehicle_ShouldHandleException_WhenReadVehicleRegNumberFails(int typeVehicle, boolean typeUser) throws Exception {
        // Arrange
        when(inputReaderUtil.readSelection()).thenReturn(typeVehicle); // Type CAR
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenThrow(new RuntimeException("Simulated exception"));

        // Act
        parkingService.processIncomingVehicle();

        // Assert
        verify(ticketDAO, never()).saveTicket(any()); // rien n'est enregistré
        verify(parkingSpotDAO, never()).updateParking(any()); // aucune mise à jour
    }

    @Test
    public void processExitingVehicleTestUnableUpdateTicket(){
        when(ticketDAO.getNbTickets(REG_NUMBER)).thenReturn(false);
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

        parkingService.processExitingVehicle();

        assertNotNull(ticketDAO.getTicket(REG_NUMBER).getOutTime());
        assertTrue(ticketDAO.getTicket(REG_NUMBER).getPrice() > 0.);
        verify(parkingSpotDAO, Mockito.times(0)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
        verify(ticketDAO, Mockito.times(1)).getNbTickets(REG_NUMBER);
    }

    @ParameterizedTest
    @ValueSource(ints = {1,2})
    public void getNextParkingNumberIfAvailableTest(int number)  {
        when(inputReaderUtil.readSelection()).thenReturn(number);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);

        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        assertNotNull(parkingSpot, "Le parkingSpot retourné ne doit pas être null");

        // Vérification que le parkingSpot a le bon numéro et le bon type de véhicule
        assertEquals(1, parkingSpot.getId(), "Le numéro de parking doit être 1");
        if (number == 1) {
            assertEquals(ParkingType.CAR, parkingSpot.getParkingType(), "Le type de véhicule doit être une voiture");
        } else
            assertEquals(ParkingType.BIKE, parkingSpot.getParkingType(), "Le type de véhicule doit être une moto");

        assertTrue(parkingSpot.isAvailable(), "Le parkingSpot doit être disponible");
    }

    @Test
    public void getNextParkingNumberIfAvailableParkingNumberNotFoundTest()  {
        when(inputReaderUtil.readSelection()).thenReturn(1); // Type Car
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(-1);

        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        assertNull(parkingSpot, "Le parkingSpot retourné doit être null");
    }

    @Test
    public void getNextParkingNumberIfAvailableParkingNumberWrongArgumentTest()  {
        when(inputReaderUtil.readSelection()).thenReturn(3); // Type Illegal Argument

        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        assertNull(parkingSpot, "Le parkingSpot retourné doit être null");
    }

}
