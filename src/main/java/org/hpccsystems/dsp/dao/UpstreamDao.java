package org.hpccsystems.dsp.dao;

import java.util.List;

import org.hpccsystems.dermatology.domain.Dermatology;
import org.hpccsystems.dsp.dashboard.entity.StaticData;

public interface UpstreamDao {

    void insertDermatology(List<Dermatology> dermatology);

    void insertStaticDataTable(List<StaticData> staticData);

}
