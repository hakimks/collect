/*
 * Copyright (C) 2012 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.tasks;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;

import org.odk.collect.android.listeners.DeleteFormsListener;
import org.odk.collect.android.provider.FormsProviderAPI.FormsColumns;

import timber.log.Timber;

/**
 * Task responsible for deleting selected forms.
 *
 * @author norman86@gmail.com
 * @author mitchellsundt@gmail.com
 */
public class DeleteFormsTask extends AsyncTask<Long, Integer, Integer> {

    private ContentResolver cr;
    private DeleteFormsListener dl;

    private int successCount;
    private int toDeleteCount;

    @Override
    protected Integer doInBackground(Long... params) {
        int deleted = 0;

        if (params == null || cr == null || dl == null) {
            return deleted;
        }
        toDeleteCount = params.length;

        // delete files from database and then from file system
        for (Long param : params) {
            if (isCancelled()) {
                break;
            }
            try {
                Uri deleteForm = Uri.withAppendedPath(FormsColumns.CONTENT_URI, param.toString());
                int wasDeleted = cr.delete(deleteForm, null, null);
                deleted += wasDeleted;

                successCount++;
                publishProgress(successCount, toDeleteCount);
            } catch (Exception ex) {
                Timber.e("Exception during delete of: %s exception: %s", param.toString(), ex.toString());
            }
        }
        successCount = deleted;
        return deleted;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        synchronized (this) {
            if (dl != null) {
                dl.progressUpdate(values[0], values[1]);
            }
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        cr = null;
        if (dl != null) {
            dl.deleteComplete(result);
        }
        super.onPostExecute(result);
    }

    @Override
    protected void onCancelled() {
        cr = null;
        if (dl != null) {
            dl.deleteComplete(successCount);
        }
    }

    public void setDeleteListener(DeleteFormsListener listener) {
        dl = listener;
    }

    public void setContentResolver(ContentResolver resolver) {
        cr = resolver;
    }

    public int getDeleteCount() {
        return successCount;
    }

    public int getToDeleteCount() {
        return toDeleteCount;
    }
}
