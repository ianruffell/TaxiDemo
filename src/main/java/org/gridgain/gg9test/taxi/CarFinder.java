package org.gridgain.gg9test.taxi;

import java.util.HashSet;
import java.util.Set;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.sql.Statement;
import org.apache.ignite.tx.Transaction;
import org.gridgain.gg9test.taxi.model.Car;
import org.gridgain.gg9test.taxi.model.TripRequest;

import com.github.javafaker.Faker;

public class CarFinder {

	private final Set<String> regs = new HashSet<>();
	private Faker faker;
	private IgniteClient ignite;

	public CarFinder(IgniteClient ignite) {
		this.ignite = ignite;
		faker = new Faker();

	}

	public Car findCarForTrip(App app, TripRequest tripRequest) {
		Car car = null;

		//new TransactionOptions().readOnly(false).
		Transaction tx = ignite.transactions().begin();
		Statement query = app.getIgnite().sql().createStatement(
				"select registration from car where tripId IS NULL ORDER BY ABS(locationId - ?) ASC LIMIT 1");
		ResultSet<SqlRow> resultSet = app.getIgnite().sql().execute(tx, query, tripRequest.getPickupLocationId());
		if (resultSet.hasNext()) {
			String reg = (String) resultSet.next().stringValue(0);
			car = app.getCarRView().get(null, Car.forId(reg));
			car.setTripId(tripRequest.getTripId());
			car.setDropOffTime(tripRequest.getDropoffDatetime());
		}
		if (car == null) {
			//System.out.println("New car");
			car = new Car(getReg(), faker.name().name(), tripRequest.getPickupLocationId(),
					tripRequest.getDropoffLocationId(), tripRequest.getRequestDatetime(),
					tripRequest.getRequestDatetime(), tripRequest.getDropoffDatetime(), tripRequest.getTripId());
		}
		app.getCarRView().upsert(tx, car);
		tx.commit();
		return car;
	}

	private String getReg() {
		String reg = faker.aviation().airport() + " " + faker.number().digits(4);
		if (regs.contains(reg)) {
			return getReg();
		} else {
			regs.add(reg);
		}

		return reg;
	}

}
