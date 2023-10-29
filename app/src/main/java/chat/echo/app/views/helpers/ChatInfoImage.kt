package chat.echo.app.views.helpers

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chat.echo.app.R
import chat.echo.app.model.*
import chat.echo.app.ui.theme.SimpleXTheme

@Composable
fun ChatInfoImage(chatInfo: ChatInfo, size: Dp, iconColor: Color = MaterialTheme.colors.secondary) {
  val icon =
    if (chatInfo is ChatInfo.Group) Icons.Filled.SupervisedUserCircle
    else Icons.Filled.AccountCircle
  ProfileImageFillWidth(size, chatInfo.image, icon, iconColor)
}

@Composable
fun ContactInfoImage(chatInfo: ChatInfo, size: Dp, iconColor: Color = MaterialTheme.colors.secondary) {
  val icon = Icons.Filled.AccountBox
  ProfileImage(size, chatInfo.image, icon, iconColor)
}

@Composable
fun GroupMemberInfoProfileImage(groupMember: GroupMember, isZeroKnowledge: Boolean, icon: ImageVector = Icons.Filled.AccountBox, size: Dp, iconColor: Color = MaterialTheme.colors.secondary) {
  if (isZeroKnowledge) {
    ProfileImageFillWidth(size, null, icon, iconColor)
  } else {
    ProfileImageFillWidth(size, groupMember.image, icon, iconColor)
  }
}

@Composable
fun GroupMemberInfoImage(groupMember: GroupMember, isZeroKnowledge: Boolean, icon: ImageVector = Icons.Filled.AccountBox, size: Dp, iconColor: Color = MaterialTheme.colors.secondary) {
  if (isZeroKnowledge) {
    ProfileImage(size = size, null, icon, iconColor)
  } else {
    ProfileImage(size, groupMember.image, icon, iconColor)
  }
}

@Composable
fun IncognitoImage(size: Dp, iconColor: Color = MaterialTheme.colors.secondary) {
  Box(Modifier.size(size)) {
    Icon(
      Icons.Filled.TheaterComedy, stringResource(R.string.incognito),
      modifier = Modifier.size(size).padding(size / 12),
      iconColor
    )
  }
}

@Composable
fun ProfileImageFillWidth(
  size: Dp,
  image: String? = null,
  icon: ImageVector = Icons.Filled.AccountBox,
  color: Color = MaterialTheme.colors.secondary
) {
  Box(
    Modifier.height(size)
      .fillMaxWidth()
  ) {
    if (image == null) {
      Icon(
        icon,
        contentDescription = stringResource(R.string.icon_descr_profile_image_placeholder),
        tint = color,
        modifier = Modifier.fillMaxSize()
      )
    } else {
      val imageBitmap = base64ToBitmap(image).asImageBitmap()
      Image(
        imageBitmap,
        stringResource(R.string.image_descr_profile_image),
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxWidth()
          .height(size)
      )
    }
  }
}

@Composable
fun ProfileImage(
  size: Dp,
  image: String? = null,
  icon: ImageVector = Icons.Filled.AccountBox,
  color: Color = MaterialTheme.colors.secondary
) {
  Box(Modifier.size(size)) {
    if (image == null) {
      Icon(
        icon,
        contentDescription = stringResource(R.string.icon_descr_profile_image_placeholder),
        tint = color,
        modifier = Modifier.fillMaxSize()
      )
    } else {
      val imageBitmap = base64ToBitmap(image).asImageBitmap()
      Image(
        imageBitmap,
        stringResource(R.string.image_descr_profile_image),
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(size).padding(size / 12).clip(RoundedCornerShape(8.dp))
      )
    }
  }
}

@Composable
fun ChatViewImage(
  size: Dp,
  image: String? = null,
  icon: ImageVector = Icons.Filled.AccountCircle,
  color: Color = MaterialTheme.colors.secondary
) {
  Box(Modifier.size(size)) {
    if (image == null) {
      Icon(
        icon,
        contentDescription = stringResource(R.string.icon_descr_profile_image_placeholder),
        tint = color,
        modifier = Modifier.fillMaxSize()
      )
    } else {
      val imageBitmap = base64ToBitmap(image).asImageBitmap()
      Image(
        imageBitmap,
        stringResource(R.string.image_descr_profile_image),
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(size).padding(size / 12).clip(CircleShape)
      )
    }
  }
}

@Preview
@Composable
fun PreviewChatInfoImage() {
  SimpleXTheme {
    ChatInfoImage(
      chatInfo = ChatInfo.Direct.sampleData,
      size = 55.dp
    )
  }
}
