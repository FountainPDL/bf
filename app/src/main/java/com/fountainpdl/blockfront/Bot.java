package com.fountainpdl.blockfront;

/** Plain data holder for one AI bot. Package-private — only GameRenderer touches it. */
class Bot {
    float x, z, yaw;
    float health;
    boolean alive = true;
    float wanderTargetX, wanderTargetZ;
    long lastShotUptimeMs = 0;
    float walkPhase = 0f;

    Bot(float x, float z, float health) {
        this.x = x;
        this.z = z;
        this.health = health;
        this.wanderTargetX = x;
        this.wanderTargetZ = z;
    }
}

