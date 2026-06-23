package it.polimi.ingsw.am54.network.messages;

import java.io.Serializable;

/**
 * Root interface for ALL messages traveling on the network.
 * Being empty, it acts as a "label" (Marker Interface) to ensure
 * that only allowed objects are sent through Sockets,
 * making them automatically Serializable.
 */
public interface ClientMessage extends Serializable {
    // No methods in here!
}