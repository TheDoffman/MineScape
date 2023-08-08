package org.hoffmantv.minescape.managers;

import org.hoffmantv.minescape.skills.MiningSkill;

public class PlayerSkills {
    private MiningSkill miningSkill;
    // ... [other skills]

    public PlayerSkills(MiningSkill miningSkill) {
        this.miningSkill = miningSkill;
        // ...
    }

    public MiningSkill getMiningSkill() {
        return miningSkill;
    }

    // ... [getters and setters for other skills]
}
