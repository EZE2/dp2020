create database Dbase

create table address \
	(addrId int, street varchar, city varchar, \
	state char(2), zip int, primary key(addrId))

insert into address values( 0,'12 MyStreet','Berkeley','CA','99999')
insert into address values( 1, '34 Quarry Ln.', 'Bedrock' , 'XX', '00000')
