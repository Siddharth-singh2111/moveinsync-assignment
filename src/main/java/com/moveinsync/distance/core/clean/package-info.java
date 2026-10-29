/**
 * Stream cleaning: dedupe pings, order by timestamp, drop low-accuracy fixes, and drop
 * "teleport" jumps whose implied speed exceeds the configured limit.
 */
package com.moveinsync.distance.core.clean;
