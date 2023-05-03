package org.folio.des.util;

import org.folio.des.domain.dto.LegacyBursarFeeFines;
import org.folio.des.domain.dto.LegacyJob;

public class LegacyBursarMigrationUtil {

  public static boolean isLegacyJob(LegacyJob job) {
    LegacyBursarFeeFines bursarFeeFines = job
      .getExportTypeSpecificParameters()
      .getBursarFeeFines();

    return !(
      bursarFeeFines.getPatronGroups().size() == 0 &&
      bursarFeeFines.getTypeMappings() == null &&
      bursarFeeFines.getDaysOutstanding() == null &&
      bursarFeeFines.getTransferAccountId() == null &&
      bursarFeeFines.getFeefineOwnerId() == null &&
      bursarFeeFines.getServicePointId() == null
    );
  }
}
