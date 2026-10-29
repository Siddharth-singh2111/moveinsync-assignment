/**
 * Framework-free distance engine. Nothing here depends on Spring, JPA or HTTP, so every
 * rule can be unit-tested in isolation. The pipeline is: clean -> anchor -> rule -> aggregate.
 */
package com.moveinsync.distance.core;
