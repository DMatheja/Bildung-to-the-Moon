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
        return parts.stream().mapToDouble(p -> p.currentFuel).sum();
    }

    public boolean hasActiveEngine() {
        return !stageEngines.isEmpty() && stageEngines.stream().noneMatch(e -> e.isDetached);
    }

    public void consumeFuel(double amount) {
        List<RocketPart> fuelTanks = parts.stream()
                .filter(p -> p.type == PartType.FUEL_TANK && p.currentFuel > 0)
                .collect(Collectors.toList());
        
        if (fuelTanks.isEmpty()) return;
        
        double consumptionPerTank = amount / fuelTanks.size();
        for (RocketPart tank : fuelTanks) {
            tank.currentFuel = Math.max(0, tank.currentFuel - consumptionPerTank);
        }
    }
}
