package com.etermax.jsonb.orm;

/*
public abstract class BaseRepositoryTest<T extends JsonbEntity> extends PostgresBaseTest {
	protected abstract BaseRepository<T> getRepository();

	protected abstract T getInstance();

	protected abstract Class<T> getClazz();

	@Test
	public void saveObject_whenSaveInstance_instanceGetsAnId() {
		T instance = getInstance();
		getRepository().save(instance);
		assertThat(instance.getId()).isGreaterThan(0L);
	}

	@Test
	public void saveObject_whenSaveInstanceTwice_instanceIdDoesNotChange() {
		T instance = getInstance();
		getRepository().save(instance);
		long id = instance.getId();

		getRepository().save(instance);

		assertThat(id).isEqualTo(instance.getId());
	}

	@Test
	public void findBy_nonExistentId_returnsNull() {
		assertThat(getRepository().findBy(getClazz(), 2999L)).isNull();
	}

	@Test
	public void findBy_existentId_returnsTheObject() {
		T instance = getInstance();
		getRepository().save(instance);
		T retrievedInstance = getRepository().findBy(getClazz(), instance.getId());
		assertThat(retrievedInstance).isNotNull();
		assertThat(retrievedInstance.getId()).isEqualToComparingFieldByField(instance.getId());
	}

	@Test
	public void findAll_withNoPersitedElements_returnsEmptyList() {
		assertThat(getRepository().findAll(getClazz())).isEmpty();
	}

	@Test
	public void findAll_withOnePersitedElement_returnsListContainingElement() {
		T instance = getInstance();
		getRepository().save(instance);

		assertThat(getRepository().findAll(getClazz()).stream().map(PostgresPersistent::getId).collect(Collectors.toList()))
				.contains(instance.getId());
	}

	@Test
	public void delete_theItemDoesNotExistAnymore() {
		T instance = getInstance();
		getRepository().save(instance);
		assertThat(getRepository().findAll(getClazz()).stream().map(PostgresPersistent::getId).collect(Collectors.toList()))
				.contains(instance.getId());

		getRepository().delete(getClazz(), instance.getId());

		assertThat(getRepository().findAll(getClazz())).isEmpty();
	}

}
*/