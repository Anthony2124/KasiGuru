import { GroundScaffold } from '../kit';
import { StoriesList } from './Library';

/** The stories on their own, pushed (Screen.StoryList). The Library tab shows the same list. */
export function StoryListScreen() {
  return (
    <GroundScaffold title="Stories" largeTitle subtitle="Stories with Tagalog and English alongside">
      <StoriesList />
    </GroundScaffold>
  );
}
