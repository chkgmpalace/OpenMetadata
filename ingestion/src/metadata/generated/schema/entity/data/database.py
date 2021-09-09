# generated by datamodel-codegen:
#   filename:  schema/entity/data/database.json
#   timestamp: 2021-09-09T17:49:00+00:00

from __future__ import annotations

from typing import Optional

from pydantic import BaseModel, Field, constr

from ...type import basic, entityReference, usageDetails


class DatabaseName(BaseModel):
    __root__: constr(regex=r'^[^.]*$', min_length=1, max_length=64) = Field(
        ..., description='Name that identifies the database.'
    )


class Database(BaseModel):
    id: Optional[basic.Uuid] = Field(
        None, description='Unique identifier that identifies this database instance.'
    )
    name: DatabaseName = Field(..., description='Name that identifies the database.')
    fullyQualifiedName: Optional[str] = Field(
        None,
        description="Name that uniquely identifies a database in the format 'ServiceName.DatabaseName'.",
    )
    description: Optional[str] = Field(
        None, description='Description of the database instance.'
    )
    href: Optional[basic.Href] = Field(
        None, description='Link to the resource corresponding to this entity.'
    )
    owner: Optional[entityReference.EntityReference] = Field(
        None, description='Owner of this database.'
    )
    service: entityReference.EntityReference = Field(
        ...,
        description='Link to the database cluster/service where this database is hosted in.',
    )
    usageSummary: Optional[usageDetails.TypeUsedToReturnUsageDetailsOfAnEntity] = Field(
        None, description='Latest usage information for this database.'
    )
    tables: Optional[entityReference.EntityReferenceList] = Field(
        None, description='References to tables in the database.'
    )
