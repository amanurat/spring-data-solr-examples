package net.petrikainulainen.spring.datasolr.todo.controller;

import net.petrikainulainen.spring.datasolr.common.util.LocaleContextHolderWrapper;
import net.petrikainulainen.spring.datasolr.todo.TodoTestUtil;
import net.petrikainulainen.spring.datasolr.todo.config.UnitTestContext;
import net.petrikainulainen.spring.datasolr.todo.document.TodoDocument;
import net.petrikainulainen.spring.datasolr.todo.dto.FieldValidationErrorDTO;
import net.petrikainulainen.spring.datasolr.todo.dto.FormValidationErrorDTO;
import net.petrikainulainen.spring.datasolr.todo.dto.TodoDTO;
import net.petrikainulainen.spring.datasolr.todo.exception.FormValidationError;
import net.petrikainulainen.spring.datasolr.todo.exception.TodoNotFoundException;
import net.petrikainulainen.spring.datasolr.todo.model.Todo;
import net.petrikainulainen.spring.datasolr.todo.service.TodoService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.FieldError;
import org.springframework.validation.Validator;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

/**
 * @author Petri Kainulainen
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {UnitTestContext.class})
public class TodoControllerTest {

    private static final String ERROR_MESSAGE_CODE_EMPTY_TITLE = "NotEmpty.title";
    private static final String ERROR_MESSAGE_CODE_EMPTY_TODO_TITLE = "NotEmpty.todo.title";
    private static final String ERROR_MESSAGE_CODE_TOO_LONG_DESCRIPTION = "Length.todo.description";

    private static final String ERROR_MESSAGE_EMPTY_TODO_TITLE = "Title cannot be empty.";
    private static final String ERROR_MESSAGE_TOO_LONG_DESCRIPTION = "The maximum length of the description is 500 characters.";

    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_TITLE = "title";

    private static final String OBJECT_NAME = "todo";

    private static final long RESULT_COUNT = 2L;
    private static final String SEARCH_TERM = "Foo";

    private TodoController controller;

    private LocaleContextHolderWrapper localeHolderWrapperMock;

    private MessageSource messageSourceMock;

    private TodoService serviceMock;

    @Resource
    private Validator validator;

    @Before
    public void setUp() {
        controller = new TodoController();

        localeHolderWrapperMock = mock(LocaleContextHolderWrapper.class);
        ReflectionTestUtils.setField(controller, "localeHolderWrapper", localeHolderWrapperMock);

        messageSourceMock = mock(MessageSource.class);
        ReflectionTestUtils.setField(controller, "messageSource", messageSourceMock);

        serviceMock = mock(TodoService.class);
        ReflectionTestUtils.setField(controller, "service", serviceMock);

        ReflectionTestUtils.setField(controller, "validator", validator);
    }

    @Test
    public void add_AllFieldsOk_ShouldReturnAddedTodo() throws FormValidationError {
        TodoDTO dto = TodoTestUtil.createDTO(null, TodoTestUtil.DESCRIPTION, TodoTestUtil.TITLE);
        Todo expected = TodoTestUtil.createModel(TodoTestUtil.ID, TodoTestUtil.DESCRIPTION, TodoTestUtil.TITLE);
        when(serviceMock.add(dto)).thenReturn(expected);

        TodoDTO actual = controller.add(dto);

        verify(serviceMock, times(1)).add(dto);
        verifyNoMoreInteractions(serviceMock);
        verifyZeroInteractions(localeHolderWrapperMock, messageSourceMock);

        assertTodo(expected, actual);
    }

    @Test(expected = FormValidationError.class)
    public void add_EmptyTodo_ShouldThrowException() throws FormValidationError {
        TodoDTO dto = TodoTestUtil.createDTO(null, "", "");

        controller.add(dto);

        verifyZeroInteractions(localeHolderWrapperMock, messageSourceMock, serviceMock);
    }

    @Test(expected = FormValidationError.class)
    public void add_TitleAndDescriptionAreTooLong_ShouldThrowException() throws FormValidationError {
        String description = TodoTestUtil.createStringWithLength(Todo.MAX_LENGTH_DESCRIPTION + 1);
        String title = TodoTestUtil.createStringWithLength(Todo.MAX_LENGTH_TITLE + 1);

        TodoDTO dto = TodoTestUtil.createDTO(null, description, title);

        controller.add(dto);

        verifyZeroInteractions(localeHolderWrapperMock, messageSourceMock, serviceMock);
    }

    @Test
    public void countSearchResults_ShouldReturnSearchResultCount() {
        when(serviceMock.countSearchResults(SEARCH_TERM)).thenReturn(RESULT_COUNT);

        long actual = controller.countSearchResults(SEARCH_TERM);

        verify(serviceMock, times(1)).countSearchResults(SEARCH_TERM);
        verifyNoMoreInteractions(serviceMock);
        verifyZeroInteractions(localeHolderWrapperMock, messageSourceMock);

        assertEquals(RESULT_COUNT, actual);
    }

    @Test
    public void deleteById_TodoIsNotFound_ShouldReturnDeletedTodo() throws TodoNotFoundException {
        Todo expected = TodoTestUtil.createModel(TodoTestUtil.ID, TodoTestUtil.DESCRIPTION, TodoTestUtil.TITLE);
        when(serviceMock.deleteById(TodoTestUtil.ID)).thenReturn(expected);

        TodoDTO actual = controller.deleteById(TodoTestUtil.ID);

        verify(serviceMock, times(1)).deleteById(TodoTestUtil.ID);
        verifyNoMoreInteractions(serviceMock);
        verifyZeroInteractions(localeHolderWrapperMock, messageSourceMock);

        assertTodo(expected, actual);
    }

    @Test(expected = TodoNotFoundException.class)
    public void deleteById_TodoIsNotFound_ShouldThrowException() throws TodoNotFoundException {
        when(serviceMock.deleteById(TodoTestUtil.ID)).thenThrow(new TodoNotFoundException(""));

        controller.deleteById(TodoTestUtil.ID);

        verify(serviceMock, times(1)).deleteById(TodoTestUtil.ID);
        verifyNoMoreInteractions(serviceMock);
        verifyZeroInteractions(localeHolderWrapperMock, messageSourceMock);
    }

    @Test
    public void findAll_ShouldReturnTodoList() {
        Todo model = TodoTestUtil.createModel(TodoTestUtil.ID, TodoTestUtil.DESCRIPTION, TodoTestUtil.TITLE);
        List<Todo> expected = createModels(model);

        when(serviceMock.findAll()).thenReturn(expected);

        List<TodoDTO> actual = controller.findAll();

        verify(serviceMock, times(1)).findAll();
        verifyNoMoreInteractions(serviceMock);
        verifyZeroInteractions(localeHolderWrapperMock, messageSourceMock);

        assertTodos(expected, actual);
    }

    @Test
    public void update_AllFieldsOk_ShouldReturnUpdatedTodo() throws FormValidationError, TodoNotFoundException {
        TodoDTO dto = TodoTestUtil.createDTO(TodoTestUtil.ID, TodoTestUtil.DESCRIPTION_UPDATED, TodoTestUtil.TITLE_UPDATED);
        Todo expected = TodoTestUtil.createModel(TodoTestUtil.ID, TodoTestUtil.DESCRIPTION, TodoTestUtil.TITLE);
        when(serviceMock.update(dto)).thenReturn(expected);

        TodoDTO actual = controller.update(dto, TodoTestUtil.ID);

        verify(serviceMock, times(1)).update(dto);
        verifyNoMoreInteractions(serviceMock);
        verifyZeroInteractions(localeHolderWrapperMock, messageSourceMock);

        assertTodo(expected, actual);
    }

    @Test(expected = FormValidationError.class)
    public void update_EmptyTodo_ShouldThrowException() throws FormValidationError, TodoNotFoundException {
        TodoDTO dto = TodoTestUtil.createDTO(TodoTestUtil.ID, "", "");

        controller.update(dto, TodoTestUtil.ID);

        verifyZeroInteractions(localeHolderWrapperMock, messageSourceMock, serviceMock);
    }

    @Test(expected = FormValidationError.class)
    public void update_TitleAndDescriptionAreTooLong_ShouldThrowException() throws FormValidationError, TodoNotFoundException {
        String description = TodoTestUtil.createStringWithLength(Todo.MAX_LENGTH_DESCRIPTION + 1);
        String title = TodoTestUtil.createStringWithLength(Todo.MAX_LENGTH_TITLE + 1);

        TodoDTO dto = TodoTestUtil.createDTO(TodoTestUtil.ID, description, title);

        controller.update(dto, TodoTestUtil.ID);

        verifyZeroInteractions(localeHolderWrapperMock, messageSourceMock, serviceMock);
    }

    @Test(expected = TodoNotFoundException.class)
    public void update_TodoIsNotFound_ShouldThrowException() throws FormValidationError, TodoNotFoundException {
        TodoDTO dto = TodoTestUtil.createDTO(TodoTestUtil.ID, TodoTestUtil.DESCRIPTION, TodoTestUtil.TITLE);
        when(serviceMock.update(dto)).thenThrow(new TodoNotFoundException(""));

        controller.update(dto, TodoTestUtil.ID);

        verify(serviceMock, times(1)).update(dto);
        verifyNoMoreInteractions(serviceMock);
        verifyZeroInteractions(localeHolderWrapperMock, messageSourceMock);
    }

    private void assertTodos(List<Todo> expected, List<TodoDTO> actual) {
        assertEquals(expected.size(), actual.size());

        for (int index = 0; index < expected.size(); index++) {
            Todo model = expected.get(index);
            TodoDTO dto = actual.get(index);
            assertTodo(model, dto);
        }
    }

    private List<Todo> createModels(Todo... models) {
        List<Todo> todos = new ArrayList<Todo>();

        for (Todo model: models) {
            todos.add(model);
        }

        return todos;
    }

    @Test
    public void search_ShouldReturnTodoList() {
        TodoDocument document = TodoTestUtil.createDocument(TodoTestUtil.ID, TodoTestUtil.DESCRIPTION, TodoTestUtil.TITLE);
        List<TodoDocument> documents = createDocuments(document);

        when(serviceMock.search(eq(SEARCH_TERM), any(Pageable.class))).thenReturn(documents);

        PageRequest page = new PageRequest(1, 1);
        List<TodoDTO> results = controller.search(SEARCH_TERM, page);

        verify(serviceMock, times(1)).search(SEARCH_TERM, page);
        verifyNoMoreInteractions(serviceMock);

        assertEquals(documents.size(), results.size());

        for (int index = 0; index < documents.size(); index++) {
            TodoDocument expected = documents.get(index);
            TodoDTO actual = results.get(index);

            assertEquals(Long.valueOf(expected.getId()), actual.getId());
            assertEquals(expected.getTitle(), actual.getTitle());
            assertNull(actual.getDescription());
        }
    }

    private List<TodoDocument> createDocuments(TodoDocument... documents) {
        List<TodoDocument> list = new ArrayList<TodoDocument>();

        for (TodoDocument document: documents) {
            list.add(document);
        }

        return list;
    }

    @Test
    public void findById_TodoIsFound_ShouldReturnTodo() throws TodoNotFoundException {
        Todo expected = TodoTestUtil.createModel(TodoTestUtil.ID, TodoTestUtil.DESCRIPTION, TodoTestUtil.TITLE);
        when(serviceMock.findById(TodoTestUtil.ID)).thenReturn(expected);

        TodoDTO actual = controller.findById(TodoTestUtil.ID);

        verify(serviceMock, times(1)).findById(TodoTestUtil.ID);
        verifyNoMoreInteractions(serviceMock);
        verifyZeroInteractions(localeHolderWrapperMock, messageSourceMock);

        assertTodo(expected, actual);
    }

    @Test(expected = TodoNotFoundException.class)
    public void findById_TodoIsNotFound_ShouldThrowException() throws TodoNotFoundException {
        when(serviceMock.findById(TodoTestUtil.ID)).thenThrow(new TodoNotFoundException(""));

        controller.findById(TodoTestUtil.ID);

        verify(serviceMock, times(1)).findById(TodoTestUtil.ID);
        verifyNoMoreInteractions(serviceMock);
        verifyZeroInteractions(localeHolderWrapperMock, messageSourceMock);
    }

    @Test
    public void handleFormValidationError_AllMessagesFound_ShouldReturnFormValidationErrors() {
        FieldError titleError = createFieldError(OBJECT_NAME, FIELD_TITLE, ERROR_MESSAGE_CODE_EMPTY_TODO_TITLE);
        FieldError descriptionError = createFieldError(OBJECT_NAME, FIELD_DESCRIPTION, ERROR_MESSAGE_CODE_TOO_LONG_DESCRIPTION);

        List<FieldError> errors = new ArrayList<FieldError>();
        errors.add(titleError);
        errors.add(descriptionError);

        FormValidationError validationError = new FormValidationError(errors);

        when(localeHolderWrapperMock.getCurrentLocale()).thenReturn(Locale.US);

        when(messageSourceMock.getMessage(ERROR_MESSAGE_CODE_EMPTY_TODO_TITLE, titleError.getArguments(), Locale.US)).thenReturn(ERROR_MESSAGE_EMPTY_TODO_TITLE);
        when(messageSourceMock.getMessage(ERROR_MESSAGE_CODE_TOO_LONG_DESCRIPTION, descriptionError.getArguments(), Locale.US)).thenReturn(ERROR_MESSAGE_TOO_LONG_DESCRIPTION);

        FormValidationErrorDTO dto = controller.handleFormValidationError(validationError);

        verify(localeHolderWrapperMock, times(1)).getCurrentLocale();
        verifyNoMoreInteractions(localeHolderWrapperMock);

        verify(messageSourceMock, times(1)).getMessage(ERROR_MESSAGE_CODE_EMPTY_TODO_TITLE, titleError.getArguments(), Locale.US);
        verify(messageSourceMock, times(1)).getMessage(ERROR_MESSAGE_CODE_TOO_LONG_DESCRIPTION, descriptionError.getArguments(), Locale.US);
        verifyNoMoreInteractions(messageSourceMock);

        verifyZeroInteractions(serviceMock);

        List<FieldValidationErrorDTO> fieldErrorDTOs = dto.getFieldErrors();

        assertEquals(2, fieldErrorDTOs.size());

        FieldValidationErrorDTO titleFieldErrorDTO = fieldErrorDTOs.get(0);
        assertEquals(FIELD_TITLE, titleFieldErrorDTO.getPath());
        assertEquals(ERROR_MESSAGE_EMPTY_TODO_TITLE, titleFieldErrorDTO.getMessage());

        FieldValidationErrorDTO descriptionFieldErrorDTO = fieldErrorDTOs.get(1);
        assertEquals(FIELD_DESCRIPTION, descriptionFieldErrorDTO.getPath());
        assertEquals(ERROR_MESSAGE_TOO_LONG_DESCRIPTION, descriptionFieldErrorDTO.getMessage());
    }

    @Test
    public void handleFormValidationError_ErrorMessageIsNotFoundWithFirstErrorCode_ShouldReturnFormValidationError() {
        FieldError titleError = createFieldError(OBJECT_NAME, FIELD_TITLE, ERROR_MESSAGE_CODE_EMPTY_TITLE, ERROR_MESSAGE_CODE_EMPTY_TODO_TITLE);

        List<FieldError> errors = new ArrayList<FieldError>();
        errors.add(titleError);

        FormValidationError validationError = new FormValidationError(errors);

        when(localeHolderWrapperMock.getCurrentLocale()).thenReturn(Locale.US);

        when(messageSourceMock.getMessage(ERROR_MESSAGE_CODE_EMPTY_TITLE, titleError.getArguments(), Locale.US)).thenReturn(ERROR_MESSAGE_CODE_EMPTY_TITLE);
        when(messageSourceMock.getMessage(ERROR_MESSAGE_CODE_EMPTY_TODO_TITLE, titleError.getArguments(), Locale.US)).thenReturn(ERROR_MESSAGE_EMPTY_TODO_TITLE);

        FormValidationErrorDTO dto = controller.handleFormValidationError(validationError);

        verify(localeHolderWrapperMock, times(1)).getCurrentLocale();
        verifyNoMoreInteractions(localeHolderWrapperMock);

        verify(messageSourceMock, times(1)).getMessage(ERROR_MESSAGE_CODE_EMPTY_TITLE, titleError.getArguments(), Locale.US);
        verify(messageSourceMock, times(1)).getMessage(ERROR_MESSAGE_CODE_EMPTY_TODO_TITLE, titleError.getArguments(), Locale.US);
        verifyNoMoreInteractions(messageSourceMock);

        verifyZeroInteractions(serviceMock);

        List<FieldValidationErrorDTO> fieldErrorDTOs = dto.getFieldErrors();

        assertEquals(1, fieldErrorDTOs.size());

        FieldValidationErrorDTO titleFieldErrorDTO = fieldErrorDTOs.get(0);
        assertEquals(FIELD_TITLE, titleFieldErrorDTO.getPath());
        assertEquals(ERROR_MESSAGE_EMPTY_TODO_TITLE, titleFieldErrorDTO.getMessage());
    }

    @Test
    public void handleFormValidationError_ErrorMessagesAreNotFound_ShouldReturnFormValidationError() {
        FieldError titleError = createFieldError(OBJECT_NAME, FIELD_TITLE, ERROR_MESSAGE_CODE_EMPTY_TODO_TITLE);
        FieldError descriptionError = createFieldError(OBJECT_NAME, FIELD_DESCRIPTION, ERROR_MESSAGE_CODE_TOO_LONG_DESCRIPTION);

        List<FieldError> errors = new ArrayList<FieldError>();
        errors.add(titleError);
        errors.add(descriptionError);

        FormValidationError validationError = new FormValidationError(errors);

        when(localeHolderWrapperMock.getCurrentLocale()).thenReturn(Locale.US);

        when(messageSourceMock.getMessage(ERROR_MESSAGE_CODE_EMPTY_TODO_TITLE, titleError.getArguments(), Locale.US)).thenReturn(ERROR_MESSAGE_CODE_EMPTY_TODO_TITLE);
        when(messageSourceMock.getMessage(ERROR_MESSAGE_CODE_TOO_LONG_DESCRIPTION, descriptionError.getArguments(), Locale.US)).thenReturn(ERROR_MESSAGE_CODE_TOO_LONG_DESCRIPTION);

        FormValidationErrorDTO dto = controller.handleFormValidationError(validationError);

        verify(localeHolderWrapperMock, times(1)).getCurrentLocale();
        verifyNoMoreInteractions(localeHolderWrapperMock);

        verify(messageSourceMock, times(1)).getMessage(ERROR_MESSAGE_CODE_EMPTY_TODO_TITLE, titleError.getArguments(), Locale.US);
        verify(messageSourceMock, times(1)).getMessage(ERROR_MESSAGE_CODE_TOO_LONG_DESCRIPTION, descriptionError.getArguments(), Locale.US);
        verifyNoMoreInteractions(messageSourceMock);

        verifyZeroInteractions(serviceMock);

        List<FieldValidationErrorDTO> fieldErrorDTOs = dto.getFieldErrors();

        assertEquals(2, fieldErrorDTOs.size());

        FieldValidationErrorDTO titleFieldErrorDTO = fieldErrorDTOs.get(0);
        assertEquals(FIELD_TITLE, titleFieldErrorDTO.getPath());
        assertEquals(ERROR_MESSAGE_CODE_EMPTY_TODO_TITLE, titleFieldErrorDTO.getMessage());

        FieldValidationErrorDTO descriptionFieldErrorDTO = fieldErrorDTOs.get(1);
        assertEquals(FIELD_DESCRIPTION, descriptionFieldErrorDTO.getPath());
        assertEquals(ERROR_MESSAGE_CODE_TOO_LONG_DESCRIPTION, descriptionFieldErrorDTO.getMessage());
    }

    @Test
    public void handleFormValidationError_ErrorMessagesAreNull_ShouldReturnFormValidationErrorWitNullErrorMessages() {
        FieldError titleError = createFieldError(OBJECT_NAME, FIELD_TITLE, ERROR_MESSAGE_CODE_EMPTY_TODO_TITLE);
        FieldError descriptionError = createFieldError(OBJECT_NAME, FIELD_DESCRIPTION, ERROR_MESSAGE_CODE_TOO_LONG_DESCRIPTION);

        List<FieldError> errors = new ArrayList<FieldError>();
        errors.add(titleError);
        errors.add(descriptionError);

        FormValidationError validationError = new FormValidationError(errors);

        when(localeHolderWrapperMock.getCurrentLocale()).thenReturn(Locale.US);

        when(messageSourceMock.getMessage(ERROR_MESSAGE_CODE_EMPTY_TODO_TITLE, titleError.getArguments(), Locale.US)).thenReturn(null);
        when(messageSourceMock.getMessage(ERROR_MESSAGE_CODE_TOO_LONG_DESCRIPTION, descriptionError.getArguments(), Locale.US)).thenReturn(null);

        FormValidationErrorDTO dto = controller.handleFormValidationError(validationError);

        verify(localeHolderWrapperMock, times(1)).getCurrentLocale();
        verifyNoMoreInteractions(localeHolderWrapperMock);

        verify(messageSourceMock, times(1)).getMessage(ERROR_MESSAGE_CODE_EMPTY_TODO_TITLE, titleError.getArguments(), Locale.US);
        verify(messageSourceMock, times(1)).getMessage(ERROR_MESSAGE_CODE_TOO_LONG_DESCRIPTION, descriptionError.getArguments(), Locale.US);
        verifyNoMoreInteractions(messageSourceMock);

        verifyZeroInteractions(serviceMock);

        List<FieldValidationErrorDTO> fieldErrorDTOs = dto.getFieldErrors();

        assertEquals(2, fieldErrorDTOs.size());

        FieldValidationErrorDTO titleFieldErrorDTO = fieldErrorDTOs.get(0);
        assertEquals(FIELD_TITLE, titleFieldErrorDTO.getPath());
        assertNull(titleFieldErrorDTO.getMessage());

        FieldValidationErrorDTO descriptionFieldErrorDTO = fieldErrorDTOs.get(1);
        assertEquals(FIELD_DESCRIPTION, descriptionFieldErrorDTO.getPath());
        assertNull(descriptionFieldErrorDTO.getMessage());
    }

    private void assertTodo(Todo expected, TodoDTO actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getTitle(), actual.getTitle());
    }

    private FieldError createFieldError(String objectName, String path, String... errorMessageCodes) {
        return new FieldError(objectName,
                path,
                null,
                false,
                errorMessageCodes,
                new Object[]{},
                errorMessageCodes[0]);
    }
}
