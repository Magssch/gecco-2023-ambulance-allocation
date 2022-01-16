package no.ntnu.ambulanceallocation.simulation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.PriorityQueue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import no.ntnu.ambulanceallocation.simulation.event.Event;
import no.ntnu.ambulanceallocation.simulation.event.JobCompletion;
import no.ntnu.ambulanceallocation.simulation.event.NewCall;
import no.ntnu.ambulanceallocation.simulation.incident.Incident;
import no.ntnu.ambulanceallocation.simulation.incident.IncidentIO;

public class PriorityQueueTest {

    private static final PriorityQueue<Event> eventQueue = new PriorityQueue<>();
    private static final LocalDateTime startDateTime = LocalDateTime.of(2018, 1, 1, 0, 0, 0);
    private static final LocalDateTime endDateTime = LocalDateTime.of(2019, 1, 1, 0, 0, 0);
    private static final List<Incident> incidents = IncidentIO.loadIncidentsFromFile(startDateTime, endDateTime);

    @BeforeAll
    public static void setup() {
        eventQueue.clear();
        eventQueue.addAll(incidents.stream().map(NewCall::new).toList());
    }

    @Test
    public void shouldPollEventsInCorrectOrder() {
        boolean testResult = true;

        while (eventQueue.size() > 2) {
            Event event = eventQueue.poll();
            Event nextEvent = eventQueue.poll();

            if (event.getTime().isAfter(nextEvent.getTime())) {
                testResult = false;
                break;
            }

        }

        assertTrue(testResult);
    }

    @Test
    public void shouldPollEventsInCorrectOrderWhenInserted() {
        for (int i = 0; i < 30; i++) {
            eventQueue.add(new JobCompletion(startDateTime.plusSeconds(40 * i + i), null));
        }

        boolean testResult = true;

        while (eventQueue.size() > 2) {
            Event event = eventQueue.poll();
            Event nextEvent = eventQueue.poll();

            if (event.getTime().isAfter(nextEvent.getTime())) {
                testResult = false;
                break;
            }

        }

        assertTrue(testResult);
    }

}
