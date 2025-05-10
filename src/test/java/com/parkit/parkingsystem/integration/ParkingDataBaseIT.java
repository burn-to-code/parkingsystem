package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static final String REG_NUMBER = "ABCDEF";

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
        lenient().when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(REG_NUMBER);
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    public static void tearDown(){ dataBasePrepareService.clearDataBaseEntries(); }

    public Ticket setIntimeTicketDao(long time) {
        Ticket ticket = ticketDAO.getTicket(REG_NUMBER);
        return setIntimeTicketDao(ticket, time);
    }

    public Ticket setIntimeTicketDao(Ticket ticket, long time) {
        Date inTime = new Date(System.currentTimeMillis() - (time)); // 60 minutes de simulation
        ticket.setInTime(inTime);
        ticketDAO.saveTicket(ticket);
        return ticket;
    }


    @Test
    public void testParkingACar(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        parkingService.processIncomingVehicle();

        Ticket ticket = ticketDAO.getTicket(REG_NUMBER);
        ParkingSpot updatedSpot = ticket.getParkingSpot();

        Assertions.assertNotNull(ticket, "Une ticket doit être enregistré en base de donnée !");
        assertEquals(REG_NUMBER, ticket.getVehicleRegNumber());
        Assertions.assertNotNull(ticket.getInTime(), "Un temps d'entrée doit être stipulé !");
        Assertions.assertFalse(updatedSpot.isAvailable(), "le parking spot du ticket en data doit être occupé !");
    }


    @Test
    public void testParkingLotExit() {

        //SIMULER L'ENTRE
//        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // SIMULER UNE ENTREE 60 MINUTES AVANT
        Ticket ticket = new Ticket();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber(REG_NUMBER);
        Date inTime = new Date(System.currentTimeMillis() - (60*60*1000)); // 60 minutes de simulation
        ticket.setInTime(inTime);
        ticketDAO.saveTicket(ticket);

        // SIMULER LA SORTIE
        parkingService.processExitingVehicle();

        // TESTER SI L'HEURE DE SORTIE EST CORRECT, SI LE PRIX EST EXISTANT ET CORRECT EN DATABASE
        Ticket updateTicket = ticketDAO.getTicket(REG_NUMBER);
        double resultTime = (updateTicket.getOutTime().getTime() - updateTicket.getInTime().getTime()) / (1_000.0 * 60.0 * 60.0);
        double expectedResult = Math.round((resultTime * Fare.CAR_RATE_PER_HOUR) * 100.0) / 100.0;

        Assertions.assertNotNull(updateTicket.getOutTime(),"OutTime should be set");
        Assertions.assertEquals(expectedResult,updateTicket.getPrice(), "Price should be set"); // délai a cause des miliseconde du test
    }

    @Test
    public void parkingLotExitRecurringUserTest() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);


        Ticket ticket = new Ticket();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber(REG_NUMBER);
        Date inTime = new Date(System.currentTimeMillis() - (24*60*60*1000)); // 24 heures avant
        ticket.setInTime(inTime);
        Date outTime = new Date(System.currentTimeMillis() - (23*60*60*1000));
        ticket.setOutTime(outTime);
        ticketDAO.saveTicket(ticket);

        Assertions.assertTrue(ticketDAO.getNbTickets(REG_NUMBER));

        setIntimeTicketDao(60*60*1000);
        parkingService.processExitingVehicle();

        Ticket verifiedTicket = ticketDAO.getTicket(REG_NUMBER);
        double resultTime = (verifiedTicket.getOutTime().getTime() - verifiedTicket.getInTime().getTime()) / (1_000.0 * 60.0 * 60.0);
        double expectedFare = Math.round((resultTime * Fare.CAR_RATE_PER_HOUR * 0.95) * 100.0) / 100.0;
        System.out.printf("Expected fare: %.10f%n", expectedFare);
        System.out.printf("Actual fare:   %.10f%n", verifiedTicket.getPrice());
        assertEquals(expectedFare, verifiedTicket.getPrice(), 0.011);
    }
}
