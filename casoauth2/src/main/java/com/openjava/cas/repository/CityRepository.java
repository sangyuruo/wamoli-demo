package com.openjava.cas.repository;

import com.openjava.cas.domain.City;
import org.springframework.stereotype.Repository;

import com.datastax.driver.core.*;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Cassandra repository for the City entity.
 */
@Repository
public class CityRepository {

    private final Session session;

    private final Validator validator;

    private Mapper<City> mapper;

    private PreparedStatement findAllStmt;

    private PreparedStatement truncateStmt;

    public CityRepository(Session session, Validator validator) {
        this.session = session;
        this.validator = validator;
        this.mapper = new MappingManager(session).mapper(City.class);
        this.findAllStmt = session.prepare("SELECT * FROM city");
        this.truncateStmt = session.prepare("TRUNCATE city");
    }

    public List<City> findAll() {
        List<City> citiesList = new ArrayList<>();
        BoundStatement stmt = findAllStmt.bind();
        session.execute(stmt).all().stream().map(
            row -> {
                City city = new City();
                city.setId(row.getUUID("id"));
                city.setName(row.getString("name"));
                city.setCountry(row.getString("country"));
                return city;
            }
        ).forEach(citiesList::add);
        return citiesList;
    }

    public City findOne(UUID id) {
        return mapper.get(id);
    }

    public City save(City city) {
        if (city.getId() == null) {
            city.setId(UUID.randomUUID());
        }
        Set<ConstraintViolation<City>> violations = validator.validate(city);
        if (violations != null && !violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        mapper.save(city);
        return city;
    }

    public void delete(UUID id) {
        mapper.delete(id);
    }

    public void deleteAll() {
        BoundStatement stmt = truncateStmt.bind();
        session.execute(stmt);
    }
}
