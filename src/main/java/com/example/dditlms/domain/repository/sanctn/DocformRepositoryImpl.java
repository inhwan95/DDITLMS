package com.example.dditlms.domain.repository.sanctn;

import com.example.dditlms.domain.entity.sanction.DocFormCategory;
import com.querydsl.jpa.impl.JPAQueryFactory;

import javax.persistence.EntityManager;

import java.util.List;

import static com.example.dditlms.domain.entity.sanction.QDocform.docform;

public class DocformRepositoryImpl implements DocformRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    public DocformRepositoryImpl(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public List<DocFormCategory> allDocFormList() {

        return queryFactory
                .selectDistinct(docform.docFormCategory)
                .from(docform)
                .fetch();

    }
}
