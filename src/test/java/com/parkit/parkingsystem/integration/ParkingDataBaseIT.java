package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests d'intégration sur la gestion du parking")
public class ParkingDataBaseIT {

    private static final String REG_NUMBER = "ABCDEF";
    private static final int ONE_HOURS_IN_MILLIS= 60*60*1000;

    private static final DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    // ----- SETUP -----

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
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(REG_NUMBER);
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    public static void tearDown(){ dataBasePrepareService.clearDataBaseEntries(); }

    // ----- DATA SOURCE ------

    static Stream<Arguments> calculateFareSource(){
        return Stream.of(
                Arguments.of(1 , ParkingType.CAR , Fare.CAR_RATE_PER_HOUR, 1),
                Arguments.of(2 , ParkingType.CAR , Fare.CAR_RATE_PER_HOUR, 1),
                Arguments.of(3 , ParkingType.CAR , Fare.CAR_RATE_PER_HOUR, 1),
                Arguments.of(4 , ParkingType.CAR , Fare.CAR_RATE_PER_HOUR, 1),
                Arguments.of(5 , ParkingType.CAR , Fare.CAR_RATE_PER_HOUR, 1),
                Arguments.of(6, ParkingType.CAR , Fare.CAR_RATE_PER_HOUR, 1),
                Arguments.of(7 , ParkingType.CAR , Fare.CAR_RATE_PER_HOUR, 1),
                Arguments.of(8 , ParkingType.CAR , Fare.CAR_RATE_PER_HOUR, 1),
                Arguments.of(9 , ParkingType.CAR , Fare.CAR_RATE_PER_HOUR, 1),
                Arguments.of(1 , ParkingType.BIKE , Fare.BIKE_RATE_PER_HOUR, 4),
                Arguments.of(2 , ParkingType.BIKE , Fare.BIKE_RATE_PER_HOUR, 4),
                Arguments.of(3 , ParkingType.BIKE , Fare.BIKE_RATE_PER_HOUR, 4),
                Arguments.of(4 , ParkingType.BIKE , Fare.BIKE_RATE_PER_HOUR, 4),
                Arguments.of(5 , ParkingType.BIKE , Fare.BIKE_RATE_PER_HOUR, 4),
                Arguments.of(6, ParkingType.BIKE , Fare.BIKE_RATE_PER_HOUR, 4),
                Arguments.of(7 , ParkingType.BIKE , Fare.BIKE_RATE_PER_HOUR, 4),
                Arguments.of(8 , ParkingType.BIKE , Fare.BIKE_RATE_PER_HOUR, 4),
                Arguments.of(9 , ParkingType.BIKE , Fare.BIKE_RATE_PER_HOUR, 4)
        );
    }

    // ----- START TESTS -----

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    @DisplayName("Enregistrer un véhicule entrant et vérifier son ticket et la disponibilité du spot")
    public void testParkingComing(int choice){
        // GIVEN
        final ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        when(inputReaderUtil.readSelection()).thenReturn(choice);

        // WHEN
        parkingService.processIncomingVehicle();

        // THEN
        final Ticket ticket = ticketDAO.getTicket(REG_NUMBER);
        ParkingSpot updatedSpot = ticket.getParkingSpot();

        assertNotNull(ticket, "Une ticket doit être enregistré en base de donnée !");
        assertEquals(REG_NUMBER, ticket.getVehicleRegNumber());
        assertNotNull(ticket.getInTime(), "Un temps d'entrée doit être stipulé !");
        Assertions.assertFalse(updatedSpot.isAvailable(), "le parking spot du ticket en data doit être occupé !");
    }


    @ParameterizedTest(name = "Vérification du tarif pour une durée de stationnement de {0}h")
    @MethodSource("calculateFareSource")
    @DisplayName("Calcul du tarif de sortie selon la durée de stationnement")
    public void testParkingLotExit(int hours, ParkingType parkingType, double fare, int place) {

        //GIVEN
        final ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        Ticket ticket = new Ticket();
        ParkingSpot parkingSpot = new ParkingSpot(place, parkingType, false);
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber(REG_NUMBER);
        Date inTime = new Date(System.currentTimeMillis() - ((long) hours * ONE_HOURS_IN_MILLIS)); // 60 minutes de simulation
        ticket.setInTime(inTime);
        ticketDAO.saveTicket(ticket);

        // WHEN
        parkingService.processExitingVehicle();

        // THEN
        Ticket updateTicket = ticketDAO.getTicket(REG_NUMBER);
        final double resultTime = calculateDurationInHours(updateTicket);
        final double expectedResult = Math.round((resultTime * fare) * 100.0) / 100.0;

        assertNotNull(updateTicket.getOutTime(),"OutTime should be set");
        assertEquals(expectedResult,updateTicket.getPrice(), "Price should be set");
    }

    @ParameterizedTest(name = "Vérification du tarif pour utilisateur récurrent après {0}h de stationnement")
    @MethodSource("calculateFareSource")
    @DisplayName("Réduction pour utilisateur récurrent")
    public void parkingLotExitRecurringUserTest(int hours, ParkingType parkingType, double fare, int place) throws InterruptedException {
        // GIVEN
        final ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        final long now = System.currentTimeMillis();
        final double expectedFare;
        ParkingSpot parkingSpot = new ParkingSpot(place, parkingType, false);

        // WHEN
        Ticket ticket = new Ticket();
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber(REG_NUMBER);
        Date inTime = new Date(now - (24 * ONE_HOURS_IN_MILLIS)); // 24 heures avant
        ticket.setInTime(inTime);
        Date outTime = new Date(now - (23 * ONE_HOURS_IN_MILLIS));
        ticket.setOutTime(outTime);
        ticketDAO.saveTicket(ticket);

        /// TODO : Résoudre le problème d'attente en passant une clock a ParkingSystem et à notre test par exemple, en attendant on peut mettre un delta ou un Thread
        // Thread.sleep(1000); // ATTENTE OBLIGATOIRE POUR NE PAS AVOIR D'ERREUR DE CALCUL

        // THEN
        Assertions.assertTrue(ticketDAO.getNbTickets(REG_NUMBER));

        // GIVEN
        Ticket newTicket = new Ticket();
        newTicket.setParkingSpot(parkingSpot);
        newTicket.setVehicleRegNumber(REG_NUMBER);
        Date newInTime = new Date(now - ((long) hours *ONE_HOURS_IN_MILLIS));
        newTicket.setInTime(newInTime);
        ticketDAO.saveTicket(newTicket);

        // WHEN
        parkingService.processExitingVehicle();

        // THEN
        Ticket verifiedTicket = ticketDAO.getTicket(REG_NUMBER);
        double resultTime = calculateDurationInHours(verifiedTicket);
        expectedFare = FareCalculatorService.round(resultTime * fare * 0.95 );

        assertEquals(expectedFare, verifiedTicket.getPrice(), 0.011);
    }

    // ----- HELPER -----

    private double calculateDurationInHours(Ticket ticket) {
        return (ticket.getOutTime().getTime() - ticket.getInTime().getTime()) / (1_000.0 * 60.0 * 60.0);
    }
}
