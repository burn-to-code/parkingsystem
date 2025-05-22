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
@DisplayName("Integration Tests on Parking Management")
public class ParkingDataBaseIT {

    private static final String REG_NUMBER = "ABCDEF";
    private static final int ONE_HOURS_IN_MILLIS= 60*60*1000;

    private static final DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    // ----- SETUP -----

    @Mock
    private InputReaderUtil inputReaderUtil;

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
                Arguments.of(0.2 , ParkingType.CAR , Fare.CAR_RATE_PER_HOUR, 1),
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
    @DisplayName("Register an incoming vehicle and verify its ticket and parking spot availability")
    public void testParkingComing(int choice){
        // GIVEN
        final ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        when(inputReaderUtil.readSelection()).thenReturn(choice);

        // WHEN
        parkingService.processIncomingVehicle();

        // THEN
        final Ticket ticket = ticketDAO.getTicket(REG_NUMBER);
        ParkingSpot updatedSpot = ticket.getParkingSpot();

        assertNotNull(ticket, "A ticket must be recorded in the database!");
        assertEquals(REG_NUMBER, ticket.getVehicleRegNumber());
        assertNotNull(ticket.getInTime(), "An entry time must be specified!");
        assertEquals(0, ticket.getPrice(), "The price of the ticket must be zero");
        Assertions.assertFalse(updatedSpot.isAvailable(), "The parking spot in the ticket must be occupied!");
    }


    @ParameterizedTest(name = "Check fare for a parking duration of {0}h")
    @MethodSource("calculateFareSource")
    @DisplayName("Calculate exit fare based on parking duration")
    public void testParkingLotExit(double hours, ParkingType parkingType, double fare, int place) {

        //GIVEN
        final ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        final double expectedResult;

        Ticket ticket = new Ticket();
        ParkingSpot parkingSpot = new ParkingSpot(place, parkingType, false);
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber(REG_NUMBER);
        Date inTime = new Date((long) (System.currentTimeMillis() - (hours * ONE_HOURS_IN_MILLIS)));
        ticket.setInTime(inTime);
        ticketDAO.saveTicket(ticket);

        // WHEN
        parkingService.processExitingVehicle();

        // THEN
        Ticket updateTicket = ticketDAO.getTicket(REG_NUMBER);
        final double resultTime = calculateDurationInHours(updateTicket);
        if (resultTime < 0.5) {
            expectedResult = 0;
        } else {
            expectedResult = Math.round((resultTime * fare) * 100.0) / 100.0;
        }

        assertNotNull(updateTicket.getOutTime(),"OutTime should be set");
        assertEquals(expectedResult,updateTicket.getPrice(), "Price should be set");
    }

    @ParameterizedTest(name = "Check fare for recurring user after {0}h of parking")
    @MethodSource("calculateFareSource")
    @DisplayName("Discount for recurring user")
    public void parkingLotExitRecurringUserTest(double hours, ParkingType parkingType, double fare, int place) {
        // GIVEN (first visit)
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

        // GIVEN (second visit)
        Ticket newTicket = new Ticket();
        newTicket.setParkingSpot(parkingSpot);
        newTicket.setVehicleRegNumber(REG_NUMBER);
        Date newInTime = new Date((long) (now - (hours * ONE_HOURS_IN_MILLIS)));
        newTicket.setInTime(newInTime);
        ticketDAO.saveTicket(newTicket);

        // WHEN
        parkingService.processExitingVehicle();

        // THEN
        Ticket verifiedTicket = ticketDAO.getTicket(REG_NUMBER);
        double resultTime = calculateDurationInHours(verifiedTicket);
        if (resultTime < 0.5) {
            expectedFare = 0;
        } else {
            expectedFare = FareCalculatorService.round(resultTime * fare * 0.95 );
        }

        assertEquals(expectedFare, verifiedTicket.getPrice(), 0.011);
    }

    // ----- HELPER -----

    private double calculateDurationInHours(Ticket ticket) {
        return (ticket.getOutTime().getTime() - ticket.getInTime().getTime()) / (1_000.0 * 60.0 * 60.0);
    }
}
