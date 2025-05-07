package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.apache.logging.log4j.core.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.logging.LoggerFactory;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static final DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    public static void tearDown(){
        dataBasePrepareService.clearDataBaseEntries();
    }


    @Test
    public void testParkingACar(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        parkingService.processIncomingVehicle();

        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        Assertions.assertNotNull(ticket, "Ticket should have been created in DB");
        assertEquals("ABCDEF", ticket.getVehicleRegNumber());
        Assertions.assertNotNull(ticket.getInTime(), "InTime should be set");
        ParkingSpot updatedSpot = parkingSpotDAO.getParkingSpotById(ticket.getParkingSpot().getId());
        Assertions.assertFalse(updatedSpot.isAvailable(), "Parking spot should be marked as unavailable in DB");
        //TODO: check that a ticket is actualy saved in DB and Parking table is updated with availability
    }


    @Test
    public void testParkingLotExit() {
        //SIMULER L'ENTRE
        testParkingACar();

        // SIMULER UNE ENTREE 60 MINUTES AVANT
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        Date inTime = new Date(System.currentTimeMillis() - (60*60*1000)); // 60 minutes de simulation
        ticket.setInTime(inTime);
        ticketDAO.saveTicket(ticket);

        // SIMULER LA SORTIE
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();

        // TESTER SI L'HEURE DE SORTIE EST CORRECT, SI LE PRIX EST EXISTANT EN DATABASE ET SI LA PLACE DE PARKING EST LIBERER
        Ticket updateTicket = ticketDAO.getTicket("ABCDEF");
        Assertions.assertNotNull(updateTicket.getOutTime(),"OutTime should be set");
        Assertions.assertTrue(updateTicket.getPrice() > 0, "Price should be set");
        ParkingSpot updatedSpot = parkingSpotDAO.getParkingSpotById(ticket.getParkingSpot().getId()); // On va chercher directement dans la base de donnée pour être sur
        Assertions.assertTrue(updatedSpot.isAvailable(), "Parking spot should be marked as unavailable in DB");

        //TODO: check that the fare generated and out time are populated correctly in the database
    }

    @Test
    public void parkingLotExitRecurringUserTest() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        parkingService.processIncomingVehicle();

        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        ticket.setInTime(new Date(System.currentTimeMillis() - 60 * 60 * 1000));
        ticketDAO.saveTicket(ticket);

        parkingService.processExitingVehicle();

        Assertions.assertTrue(ticketDAO.getNbTickets("ABCDEF"));

        parkingService.processIncomingVehicle();

        Ticket anotherTicket = ticketDAO.getTicket("ABCDEF");
        anotherTicket.setInTime(new Date(System.currentTimeMillis() - 60 * 60 * 1000));
        ticketDAO.saveTicket(anotherTicket);

        parkingService.processExitingVehicle();

        Ticket verifiedTicket = ticketDAO.getTicket("ABCDEF");
        double expectedFare = Fare.CAR_RATE_PER_HOUR * 0.95;
        assertEquals(expectedFare, verifiedTicket.getPrice(), 0.01);
    }
}
