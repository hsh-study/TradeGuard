package seokhoon.trade.adapter.persistence;

import jakarta.persistence.*;
import seokhoon.trade.domain.market.Sector;
import seokhoon.trade.domain.market.SectorType;

import java.time.Instant;

@Entity
@Table(name = "sectors", uniqueConstraints = @UniqueConstraint(
        name = "uk_sector_code", columnNames = "sector_code"))
public class SectorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "sector_code", nullable = false, length = 50)
    private String sectorCode;
    @Column(name = "sector_name", nullable = false, length = 100)
    private String sectorName;
    @Enumerated(EnumType.STRING)
    @Column(name = "sector_type", nullable = false, length = 30)
    private SectorType sectorType;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SectorEntity() {
    }

    static SectorEntity from(Sector value) {
        SectorEntity entity = new SectorEntity();
        entity.id = value.id();
        entity.update(value);
        return entity;
    }

    void update(Sector value) {
        sectorCode = value.sectorCode();
        sectorName = value.sectorName();
        sectorType = value.sectorType();
        createdAt = value.createdAt();
        updatedAt = value.updatedAt();
    }

    Sector toDomain() {
        return new Sector(id, sectorCode, sectorName, sectorType, createdAt, updatedAt);
    }
}
