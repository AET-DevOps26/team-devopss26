package de.tum.devopss26.noteservice.service;

import org.openapitools.model.CreateNoteRequest;
import org.openapitools.model.CreateNoteResponse;
import org.openapitools.model.GetNoteResponse;
import org.openapitools.model.ListNotesResponse;
import org.openapitools.model.UpdateNoteResponse;

public interface NoteService {

    CreateNoteResponse createNote(CreateNoteRequest request, long userId);

    ListNotesResponse getNotes(long userId);

    GetNoteResponse getNote(long userId, long id);

    UpdateNoteResponse updateNote(long userId, long id, org.openapitools.model.Note diff);

    void deleteNote(long userId, long id);
}
