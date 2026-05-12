package org.smm.archetype.auth.internal;

import java.util.Optional;

interface UserRepository {

    Optional<User> findByUsername(String username);
}
