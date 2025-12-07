package de.bildung.moon.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Datenklasse zur Speicherung einer Gruppe von Teilen, die eine Stufe bilden.
 */
public class Stage {
    public List<RocketPart> stageEngines = new ArrayList<>();
    public List<RocketPart> parts = new ArrayList<>();

    public Stage(List<RocketPart> engines) {
        this.stageEngines.addAll(engines);
    }

    public Stage() {
    }

    public double getTotalCurrentFuel() {
        double sum = 0;
        for(RocketPart part : parts){
            sum += part.currentFuel;
        }
        return sum;
    }

    public boolean hasActiveEngine() {
        if (stageEngines.isEmpty()) {
            return false;
        }

        for (RocketPart e : stageEngines) {
            if (e.isDetached) {
                return false;
            }
        }
        // List is not empty and no detached engines were found
        return true;
    }

    public void consumeFuel(double amount) {
        List<RocketPart> fuelTanks = new ArrayList<>();
        for (RocketPart p : parts) {
            if (p.category == PartType.FUEL_TANK && p.currentFuel > 0) {
                fuelTanks.add(p);
            }
        }

        if (fuelTanks.isEmpty()) {
            return;
        }

        double consumptionPerTank = amount / fuelTanks.size();

        for (RocketPart tank : fuelTanks) {
            tank.currentFuel = Math.max(0, tank.currentFuel - consumptionPerTank);
        }
    }

}
