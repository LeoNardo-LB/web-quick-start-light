package org.smm.archetype.auth.internal;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findByUsername(String username);
}
