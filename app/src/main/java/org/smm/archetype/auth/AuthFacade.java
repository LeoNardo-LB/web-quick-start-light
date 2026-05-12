package org.smm.archetype.auth;

public interface AuthFacade {
    String login(String username, String password);
    void logout();
}
