package org.gridgain.gg9test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.RecordView;
import org.apache.parquet.avro.AvroParquetReader;
import org.gridgain.gg9test.taxi.model.Trip;

public class DataLoader {

	public static final String TRIP_DATA_FILE = "src/main/resources/fhvhv_tripdata_2023-07.parquet";
	public static final int BATCH_SIZE = 10000;
	public static final int ROWS_TO_LOAD = 3000000;

	public static void main(String[] args) throws Exception {
		new DataLoader();
	}

	public DataLoader() throws Exception {

		List<Trip> trips = new ArrayList<>();
		try (IgniteClient ignite = IgniteClient.builder().addresses("127.0.0.1:10800").build()) {

			ignite.catalog().create(Trip.class).execute();

			// access
			RecordView<Trip> recordView = ignite.tables().table("trip").recordView(Trip.class);

			int count = 0;
			long start = System.currentTimeMillis();

			GenericRecord record;
			AvroParquetReader<GenericRecord> reader = getTripReader();
			while ((record = reader.read()) != null) {
				Trip trip = Trip.fromRecord(record);
				trips.add(trip);
				count++;
				if (trips.size() == BATCH_SIZE) {
					recordView.insertAll(null, trips);
					System.out.printf("Loaded %d trips\n", count);
					trips.clear();
					if (count >= ROWS_TO_LOAD) {
						break;
					}
				}
			}
			recordView.insertAll(null, trips);
			System.out.printf("Loaded %d trips in %d secs\n", count, ((System.currentTimeMillis() - start)/1000));
		}
	}

	public static AvroParquetReader<GenericRecord> getTripReader() throws IllegalArgumentException, IOException {
		@SuppressWarnings("deprecation")
		AvroParquetReader<GenericRecord> reader = new AvroParquetReader<GenericRecord>(new Configuration(),
				new Path(TRIP_DATA_FILE));

		return reader;

	}

}
