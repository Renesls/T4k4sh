package com.t4kash.api.marketplace.service;

import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.marketplace.dto.AttachmentResponse;
import com.t4kash.api.marketplace.entity.ArchivoAdjunto;
import com.t4kash.api.marketplace.entity.Tarea;
import com.t4kash.api.marketplace.repository.ArchivoAdjuntoRepository;
import com.t4kash.api.marketplace.repository.EntregaRepository;
import com.t4kash.api.marketplace.repository.TareaRepository;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {
    @Mock
    private ArchivoAdjuntoRepository archivoRepository;
    @Mock
    private TareaRepository tareaRepository;
    @Mock
    private EntregaRepository entregaRepository;
    @Mock
    private TrabajoAsignadoRepository trabajoRepository;
    @Mock
    private ObjectStorage objectStorage;

    private AttachmentService service;

    @BeforeEach
    void setUp() {
        service = new AttachmentService(
                archivoRepository,
                tareaRepository,
                entregaRepository,
                trabajoRepository,
                objectStorage
        );
    }

    @Test
    void rejectsFilesLargerThanTenMegabytes() {
        mockOwnedTask();
        byte[] content = new byte[(int) AttachmentService.MAX_FILE_SIZE + 1];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "trabajo.pdf",
                "application/pdf",
                content
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.attachToTask(10, 1, file)
        );

        assertEquals("El archivo no puede superar los 10 MB.", error.getMessage());
        verify(objectStorage, never()).upload(any(), any(), any());
    }

    @Test
    void rejectsExecutableFiles() {
        mockOwnedTask();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "programa.exe",
                "application/octet-stream",
                new byte[]{1, 2, 3}
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.attachToTask(10, 1, file)
        );

        assertTrue(error.getMessage().contains("Tipo de archivo no permitido"));
        verify(objectStorage, never()).upload(any(), any(), any());
    }

    @Test
    void uploadsFileAndStoresOnlyMetadata() {
        mockOwnedTask();
        when(objectStorage.bucketName()).thenReturn("t4kash-attachments");
        when(archivoRepository.saveAndFlush(any(ArchivoAdjunto.class)))
                .thenAnswer(invocation -> {
                    ArchivoAdjunto attachment = invocation.getArgument(0);
                    attachment.setIdArchivo(20);
                    return attachment;
                });
        byte[] content = new byte[]{1, 2, 3, 4};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Diseño final.pdf",
                "application/pdf",
                content
        );

        AttachmentResponse response = service.attachToTask(10, 1, file);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> contentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(objectStorage).upload(
                pathCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("application/pdf"),
                contentCaptor.capture()
        );
        assertArrayEquals(content, contentCaptor.getValue());
        assertTrue(pathCaptor.getValue().startsWith("tasks/10/"));
        assertEquals(20, response.idArchivo());
        assertEquals("Diseño final.pdf", response.nombreOriginal());
        assertEquals("attachments/20/download", response.rutaDescarga());

        ArgumentCaptor<ArchivoAdjunto> metadataCaptor =
                ArgumentCaptor.forClass(ArchivoAdjunto.class);
        verify(archivoRepository).saveAndFlush(metadataCaptor.capture());
        ArchivoAdjunto metadata = metadataCaptor.getValue();
        assertEquals("t4kash-attachments", metadata.getBucketStorage());
        assertEquals(4L, metadata.getTamanoBytes());
    }

    @Test
    void rejectsTaskAttachmentsFromUsersWhoDoNotOwnTheTask() {
        mockOwnedTask();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "documento.pdf",
                "application/pdf",
                new byte[]{1, 2, 3}
        );

        assertThrows(
                ForbiddenOperationException.class,
                () -> service.attachToTask(10, 99, file)
        );
        verify(objectStorage, never()).upload(any(), any(), any());
    }

    private void mockOwnedTask() {
        Tarea task = new Tarea();
        task.setIdTarea(10);
        task.setIdCliente(1);
        when(tareaRepository.findById(10)).thenReturn(Optional.of(task));
    }
}
