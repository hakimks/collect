package org.odk.collect.android.formmanagement;

import org.junit.Before;
import org.junit.Test;
import org.odk.collect.android.forms.Form;
import org.odk.collect.android.forms.FormRepository;
import org.odk.collect.android.forms.MediaFileRepository;
import org.odk.collect.android.openrosa.api.FormListApi;
import org.odk.collect.android.openrosa.api.FormListItem;
import org.odk.collect.android.openrosa.api.ManifestFile;
import org.odk.collect.android.openrosa.api.MediaFile;
import org.odk.collect.android.support.InMemFormRepository;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.utilities.FileUtils.getMd5Hash;

@SuppressWarnings("PMD.DoubleBraceInitialization")
public class ServerFormsDetailsFetcherTest {

    private final List<FormListItem> formList = asList(
            new FormListItem("http://example.com/form-1", "form-1", "server", "md5:form-1-hash", "Form 1", null),
            new FormListItem("http://example.com/form-2", "form-2", "server", "md5:form-2-hash", "Form 2", "http://example.com/form-2-manifest")
    );

    private ServerFormsDetailsFetcher fetcher;
    private FormRepository formRepository;
    private MediaFileRepository mediaFileRepository;

    @Before
    public void setup() throws Exception {
        formRepository = new InMemFormRepository();
        mediaFileRepository = mock(MediaFileRepository.class);

        FormListApi formListAPI = mock(FormListApi.class);
        when(formListAPI.fetchFormList()).thenReturn(formList);

        when(formListAPI.fetchManifest(formList.get(1).getManifestURL())).thenReturn(new ManifestFile("manifest-2-hash", asList(
                new MediaFile("blah.txt", "md5:" + getMd5Hash(new ByteArrayInputStream("blah".getBytes())), "http://example.com/media-file")))
        );

        DiskFormsSynchronizer diskFormsSynchronizer = mock(DiskFormsSynchronizer.class);
        fetcher = new ServerFormsDetailsFetcher(formRepository, mediaFileRepository, formListAPI, diskFormsSynchronizer);
    }

    @Test
    public void whenNoFormsExist_isNew() throws Exception {
        List<ServerFormDetails> serverFormDetails = fetcher.fetchFormDetails();
        assertThat(serverFormDetails.get(0).isNotOnDevice(), is(true));
        assertThat(serverFormDetails.get(1).isNotOnDevice(), is(true));
    }

    @Test
    public void whenAFormExists_andListContainsUpdatedVersion_isUpdated() throws Exception {
        formRepository.save(new Form.Builder()
                .id(2L)
                .jrFormId("form-2")
                .md5Hash("form-2-hash-old")
                .build());

        List<ServerFormDetails> serverFormDetails = fetcher.fetchFormDetails();
        assertThat(serverFormDetails.get(1).isUpdated(), is(true));
    }

    @Test
    public void whenAFormExists_andHasNewMediaFileOnServer_isUpdated() throws Exception {
        formRepository.save(new Form.Builder()
                .id(2L)
                .jrFormId("form-2")
                .jrVersion("server")
                .md5Hash("form-2-hash")
                .build());
        when(mediaFileRepository.getAll("form-2", "server")).thenReturn(emptyList());

        List<ServerFormDetails> serverFormDetails = fetcher.fetchFormDetails();
        assertThat(serverFormDetails.get(1).isUpdated(), is(true));
    }

    @Test
    public void whenAFormExists_andHasUpdatedMediaFileOnServer_isUpdated() throws Exception {
        formRepository.save(new Form.Builder()
                .id(2L)
                .jrFormId("form-2")
                .jrVersion("server")
                .md5Hash("form-2-hash")
                .build());

        File oldMediaFile = File.createTempFile("blah", ".csv");
        writeToFile(oldMediaFile, "blah before");
        when(mediaFileRepository.getAll("form-2", "server")).thenReturn(asList(oldMediaFile));

        List<ServerFormDetails> serverFormDetails = fetcher.fetchFormDetails();
        assertThat(serverFormDetails.get(1).isUpdated(), is(true));
    }

    @Test
    public void whenAFormExists_andIsNotUpdatedOnServer_andDoesNotHaveAManifest_isNotNewOrUpdated() throws Exception {
        formRepository.save(new Form.Builder()
                .id(1L)
                .jrFormId("form-1")
                .jrVersion("server")
                .md5Hash("form-1-hash")
                .build());

        List<ServerFormDetails> serverFormDetails = fetcher.fetchFormDetails();
        assertThat(serverFormDetails.get(0).isUpdated(), is(false));
        assertThat(serverFormDetails.get(0).isNotOnDevice(), is(false));
    }

    @Test
    public void whenFormExists_isNotNewOrUpdated() throws Exception {
        formRepository.save(new Form.Builder()
                .id(2L)
                .jrFormId("form-2")
                .jrVersion("server")
                .md5Hash("form-2-hash")
                .build());

        File mediaFile = File.createTempFile("blah", ".csv");
        writeToFile(mediaFile, "blah");
        when(mediaFileRepository.getAll("form-2", "server")).thenReturn(asList(mediaFile));

        List<ServerFormDetails> serverFormDetails = fetcher.fetchFormDetails();
        assertThat(serverFormDetails.get(1).isUpdated(), is(false));
        assertThat(serverFormDetails.get(1).isNotOnDevice(), is(false));
    }

    private void writeToFile(File mediaFile, String blah) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(mediaFile));
        bw.write(blah);
        bw.close();
    }
}