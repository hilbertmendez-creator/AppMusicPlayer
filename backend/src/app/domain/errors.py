"""Domain-level errors, mapped to HTTP statuses at the API boundary."""


class NotFoundError(Exception):
    """The requested entity does not exist."""


class InvalidInputError(Exception):
    """The request is well-formed but violates a business rule."""


class LyricsUnavailableError(Exception):
    """The lyrics provider could not be reached."""
