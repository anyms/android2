from peewee import *


db = SqliteDatabase("tamil_kiddo.db")


class Book(Model):
    id = PrimaryKeyField()
    title = CharField()
