package org.gridgain.gg9test.taxi.model;

import org.apache.ignite.catalog.ZoneEngine;
import org.apache.ignite.catalog.annotations.Zone;

@Zone(value = "zone_test", replicas = 2, engine = ZoneEngine.AIMEM)
public class TaxiZone {

}
